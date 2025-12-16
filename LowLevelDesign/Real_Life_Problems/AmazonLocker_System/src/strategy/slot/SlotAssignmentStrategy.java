package strategy.slot;

public interface SlotAssignmentStrategy {

    Slot assignSlot(List<Slot> eligibleSlots);

}
