package mechanicalarms.common.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class TileArmBasic extends TileEntity implements ITickable {
    private final boolean extend = true;
    boolean isOnInput;
    float[][] rotation = new float[3][3];
    float[][] animationRotation = new float[3][3];
    private boolean hasInput;
    private BlockPos sourcePos;
    private boolean hasOutput;
    private BlockPos targetPos;
    private boolean carrying = false;
    private boolean isOnOnput;

    public TileArmBasic() {
        super();
    }

    public float[][] getAnimationRotation() {
        return animationRotation;
    }

    public float[] getRotation(int idx) {
        return rotation[idx];
    }

    @Override
    public boolean hasFastRenderer() {
        return true;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        // getUpdateTag() is called whenever the chunkdata is sent to the
        // client. In contrast getUpdatePacket() is called when the tile entity
        // itself wants to sync to the client. In many cases you want to send
        // over the same information in getUpdateTag() as in getUpdatePacket().
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        // Prepare a packet for syncing our TE to the client. Since we only have to sync the stack
        // and that's all we have we just write our entire NBT here. If you have a complex
        // tile entity that doesn't need to have all information on the client you can write
        // a more optimal NBT here.
        NBTTagCompound nbtTag = new NBTTagCompound();
        this.writeToNBT(nbtTag);
        return new SPacketUpdateTileEntity(getPos(), 1, nbtTag);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        // Here we get the packet from the server and read it into our client side tile entity
        this.readFromNBT(packet.getNbtCompound());
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        return compound;
    }

    @Override
    public void update() {
        hasInput = sourcePos != null;
        hasOutput = targetPos != null;

        Vec3d vec1 = new Vec3d(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);

        if (hasInput && !carrying) {
            Vec3d vec2 = new Vec3d(sourcePos.getX() + 0.5, sourcePos.getY() + 1, sourcePos.getZ() + 0.5);
            Vec3d combinedVec = vec2.subtract(vec1);

            if (sucess(combinedVec)) {
                this.isOnInput = true;
                this.carrying = true;
            }
        } else if (hasOutput && carrying) {
            Vec3d vec2 = new Vec3d(targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5);
            Vec3d combinedVec = vec2.subtract(vec1);
            if (sucess(combinedVec)) {
                this.isOnOnput = true;
                this.carrying = false;
            }
        } else {
            this.isOnInput = false;
            walkToIdlePosition();
        }
    }

    private boolean sucess(Vec3d combinedVec) {
        double pitch = Math.atan2(combinedVec.y, Math.sqrt(combinedVec.x * combinedVec.x + combinedVec.z * combinedVec.z));
        double yaw = Math.atan2(-combinedVec.z, combinedVec.x);

        float rotPitch = rotateX(rotation[0][0], 0.1f, (float) pitch);
        boolean pitchReached = false;
        if (rotPitch != rotation[0][0]) {
            rotation[0][0] = rotPitch;
        }

        if (rotPitch >= 0 && pitch >= 0) {
            if (rotPitch <= (pitch + 0.1f) && rotPitch >= (pitch - 0.1f)) {
                pitchReached = true;
            }
        } else if (rotPitch < 0 && pitch < 0) {
            if (rotPitch <= (pitch + 0.1f) && rotPitch >= (pitch - 0.1f)) {
                pitchReached = true;
            }
        }

        float rotYaw = rotateX(rotation[0][1], 0.1f, (float) yaw);
        boolean yawReached = false;
        if (rotYaw != rotation[0][1]) {
            rotation[0][1] = rotYaw;
        }

        if (rotYaw >= 0 && yaw >= 0) {
            if (rotYaw <= (yaw + 0.1f) && rotYaw >= (yaw - 0.1f)) {
                yawReached = true;
            }
        } else if (rotYaw < 0 && yaw < 0) {
            if (rotYaw <= (yaw + 0.1f) && rotYaw >= (yaw - 0.1f)) {
                yawReached = true;
            }
        }

        float dist = (float) combinedVec.length();
        float currentArmLength = (float) (1 + (Math.abs(2 * rotation[1][0] / Math.PI)));

        boolean distReached = false;
        double rotationAmount = rotateToReach(rotation[1][0], 0.1f, currentArmLength > dist ? 1 : -1);
        if (rotationAmount != rotation[1][0]) {
            rotation[1][0] = (float) rotationAmount;
        }
        if (currentArmLength >= (dist - 0.3f) && currentArmLength <= (dist + 0.3f)) {
            distReached = true;
        }

        /*
        float currentHandLength = (float) (0.500f + (2 * rotation[2][0] / Math.PI));
        double handWalked = rotateToReach(rotation[2][0], 0.25f, currentHandLength, dist - currentArmLength);
        if (handWalked != rotation[2][0]) {
            rotation[2][0] = (float) handWalked;
        }
        if (currentHandLength >= (dist - currentArmLength - 0.01f) && currentHandLength <= (dist - currentArmLength + 0.01f)) {
            distReached = true;
        }
        */

        if (yawReached && pitchReached && distReached) {
            this.isOnInput = true;
            this.carrying = true;
            return true;
        }
        return false;
    }

    float rotateX(float currentRotation, float angularSpeed, float targetRotation) {
        float diff = targetRotation - currentRotation;
        if (diff < -0.1D) {
            float result = currentRotation - angularSpeed;
            return Math.max(result, targetRotation);

        } else if (diff > 0.1D) {
            float result = currentRotation + angularSpeed;
            return Math.min(result, targetRotation);
        }
        return targetRotation;
    }

    float rotateToReach(float currentRotation, float angularSpeed, float targetedRotation) {
        if (targetedRotation < -0) {
            float result = currentRotation + angularSpeed;
            return Math.min(result, (float) (Math.PI / 2));
        } else if (targetedRotation > 0) {
            float result = currentRotation - angularSpeed;
            return Math.max(result, (float) (-Math.PI / 2));
        }
        return currentRotation;
    }

    void walkToIdlePosition() {
        if (rotation[0][1] >= 0.099F) {
            rotation[0][1] = rotation[0][1] - 0.1F;
        } else if (rotation[0][1] <= -0.099F) {
            rotation[0][1] = rotation[0][1] + 0.1F;
        } else {
            rotation[0][1] = 0;
        }

        if (rotation[0][0] >= 0.099F) {
            rotation[0][0] = rotation[0][0] - 0.1F;
        } else if (rotation[0][0] <= -0.099F) {
            rotation[0][0] = rotation[0][0] + 0.1F;
        } else {
            rotation[0][0] = 0;
        }

        if (rotation[1][0] >= 0.099F) {
            rotation[1][0] = rotation[1][0] - 0.1F;
        } else if (rotation[0][0] <= -0.099F) {
            rotation[1][0] = rotation[1][0] + 0.1F;
        } else {
            rotation[1][0] = 0;
        }

        if (rotation[2][0] >= 0.099F) {
            rotation[2][0] = rotation[2][0] - 0.1F;
        } else if (rotation[0][0] <= -0.099F) {
            rotation[2][0] = rotation[2][0] + 0.1F;
        } else {
            rotation[2][0] = 0;
        }
    }

    public void setSource(BlockPos sourcePos) {
        this.sourcePos = sourcePos;
    }

    public void setTarget(BlockPos targetPos) {
        this.targetPos = targetPos;
    }
}
