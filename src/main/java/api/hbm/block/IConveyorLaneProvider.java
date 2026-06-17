package api.hbm.block;

@FunctionalInterface
public interface IConveyorLaneProvider {
    double[] getLaneOffsets();
}