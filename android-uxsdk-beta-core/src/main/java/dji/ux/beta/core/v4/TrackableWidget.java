package dji.ux.beta.core.v4;

/**
 * Interface to determine if class is public and analytics should be reported
 */
public interface TrackableWidget {
    boolean shouldTrack();
}
