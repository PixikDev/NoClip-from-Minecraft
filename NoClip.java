@ModuleInfo(name = "NoClip", description = "Ходить по блокам и через стены", category = Category.MOVEMENT)
public class Noclip extends Module {
    private final List<IPacket<?>> bufferedPackets = new ArrayList<>();
    private final ModeSetting releaseMode = new ModeSetting("Мод", this, "Simple", "Simple", "Double", "Desync", "None");
    private final SliderSetting semiPackets = new SliderSetting("Кол-во пакетов", this, 2f, 1f, 15, 1f);

    private boolean semiPacketSent;
    private boolean skipReleaseOnDisable;
    @EventHandler
    public void onEvent(EventPacket eventPacket) {
        if (mc.player == null || mc.player.connection == null) return;

        IPacket<?> packet = eventPacket.getPacket();
        if (packet instanceof CPlayerPacket) {
            bufferedPackets.add(packet);
            eventPacket.setCancelled(true);
        }
    }

    @EventHandler
    public void onEvent(EventUpdate eventPacket) {
        if (mc.player == null || mc.world == null) return;
        boolean noSolidInAABB = mc.world.getStatesInArea(mc.player.getBoundingBox().shrink(0.001D))
                .noneMatch(BlockState::isSolid);
        long totalStates = mc.world.getStatesInArea(mc.player.getBoundingBox().shrink(0.001D)).count();
        long solidStates = mc.world.getStatesInArea(mc.player.getBoundingBox().shrink(0.001D)).filter(BlockState::isSolid).count();
        boolean semiInsideBlock = solidStates > 0 && solidStates < totalStates;

        if (!semiPacketSent && semiInsideBlock) {
            double x = mc.player.getPosX();
            double y = mc.player.getPosY();
            double z = mc.player.getPosZ();
            float yaw = mc.player.rotationYaw;
            float pitch = mc.player.rotationPitch;
            boolean onGround = mc.player.isOnGround();
            for (int i = 0; i < semiPackets.getValue(); i++) {
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, onGround));
            }
            semiPacketSent = true;
            return;
        }

        if (semiPacketSent && noSolidInAABB) {
            skipReleaseOnDisable = true;
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        if (!skipReleaseOnDisable && semiPacketSent) {
            if (!releaseMode.is("None")) {
                runReleaseSequence(releaseMode.getCurrentMode());
            } else {
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(mc.player.getPosX(), mc.player.getPosY(), mc.player.getPosZ(), mc.player.rotationYaw, mc.player.rotationPitch, mc.player.isOnGround()));
            }
        }

        if (mc.player != null && mc.player.connection != null && !bufferedPackets.isEmpty()) {
            for (IPacket<?> packet : bufferedPackets) {
                mc.player.connection.sendPacketWithoutEvent(packet);
            }
            bufferedPackets.clear();
        }

        super.onDisable();
    }

    @Override
    public void onEnable() {
        bufferedPackets.clear();
        semiPacketSent = false;
        skipReleaseOnDisable = false;
        super.onEnable();
    }

    private void runReleaseSequence(String mode) {
        if (mc.player == null || mc.player.connection == null) return;

        double x = mc.player.getPosX();
        double y = mc.player.getPosY();
        double z = mc.player.getPosZ();
        float yaw = mc.player.rotationYaw;
        float pitch = mc.player.rotationPitch;

        switch (mode.toLowerCase()) {
            case "simple": {
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x - 5000, y, z - 5000, yaw, pitch, false));
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, mc.player.isOnGround()));
                break;
            }
            case "double": {
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x - 5000, y, z - 5000, yaw, pitch, false));
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x + 5000, y, z + 5000, yaw, pitch, false));
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, mc.player.isOnGround()));
                break;
            }
            case "desync": {
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y + 0.0625, z, yaw, pitch, false));
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, false));
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y + 0.03125, z, yaw, pitch, true));
                mc.player.connection.sendPacket(new CPlayerPacket.PositionRotationPacket(x, y, z, yaw, pitch, mc.player.isOnGround()));
                break;
            }
            default: {
                break;
            }
        }
    }
}