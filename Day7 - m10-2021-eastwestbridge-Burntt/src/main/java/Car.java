// This is the Car. It just travels across the bridge.

public class Car extends Thread {
    private final BridgeControl control;
    private final Direction travelDirection;
    private final long travelTimeMs;

    Car(BridgeControl control, Direction travelDirection, long travelTimeMs) {
        this.control = control;
        this.travelDirection = travelDirection;
        this.travelTimeMs = travelTimeMs;
    }

    @Override
    public void run() {
        try {
            Logging.debug("Hey! I am about to travel to %s!", travelDirection.toString());
            control.onArrive(this.travelDirection);
            Logging.debug("I am travelling to %s within next %dms...", travelDirection.toString(), travelTimeMs);
            Thread.sleep(travelTimeMs);
            Logging.debug("Woo-hoo! I have travelled to %s!", travelDirection.toString());
            control.onDepart();
            Logging.debug("Bye-bye!");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
