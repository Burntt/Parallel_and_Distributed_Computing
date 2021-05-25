////////////////////////////////////////////////////////////////////////////////
// XXX: This is the interface you should implement in CarControlImpl.
////////////////////////////////////////////////////////////////////////////////
interface BridgeControl {
    // `onArrive` is called by every car that is about to enter the bridge.
    // The method must block/unblock and return only when it is safe & live
    // to enter the bridge.
    void onArrive(Direction direction) throws InterruptedException;

    // `onDepart` is called once the car has left the bridge.
    void onDepart();
}
