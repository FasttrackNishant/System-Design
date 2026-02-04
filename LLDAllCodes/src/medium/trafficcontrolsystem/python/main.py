class Direction(Enum):
    NORTH = "NORTH"
    SOUTH = "SOUTH"
    EAST = "EAST"
    WEST = "WEST"



class LightColor(Enum):
    GREEN = "GREEN"
    YELLOW = "YELLOW"
    RED = "RED"



class CentralMonitor(TrafficObserver):
    def update(self, intersection_id: int, direction: Direction, color: LightColor):
        print(f"[MONITOR] Intersection {intersection_id}: Light for {direction.value} direction changed to {color.value}.")



class TrafficObserver(ABC):
    @abstractmethod
    def update(self, intersection_id: int, direction: Direction, color: LightColor):
        pass




class EastWestGreenState(IntersectionState):
    def handle(self, context: 'IntersectionController'):
        print(f"\n--- INTERSECTION {context.get_id()}: Cycle -> East-West GREEN ---")

        # Turn East and West green, ensure North and South are red
        context.get_light(Direction.EAST).start_green()
        context.get_light(Direction.WEST).start_green()
        context.get_light(Direction.NORTH).set_color(LightColor.RED)
        context.get_light(Direction.SOUTH).set_color(LightColor.RED)

        # Wait for green light duration
        time.sleep(context.get_green_duration() / 1000.0)

        # Transition East and West to Yellow
        context.get_light(Direction.EAST).transition()
        context.get_light(Direction.WEST).transition()

        # Wait for yellow light duration
        time.sleep(context.get_yellow_duration() / 1000.0)

        # Transition East and West to Red
        context.get_light(Direction.EAST).transition()
        context.get_light(Direction.WEST).transition()

        # Change the intersection's state back to let North-South go
        context.set_state(NorthSouthGreenState())







class IntersectionState(ABC):
    @abstractmethod
    def handle(self, context: 'IntersectionController'):
        pass





class NorthSouthGreenState(IntersectionState):
    def handle(self, context: 'IntersectionController'):
        print(f"\n--- INTERSECTION {context.get_id()}: Cycle Start -> North-South GREEN ---")

        # Turn North and South green, ensure East and West are red
        context.get_light(Direction.NORTH).start_green()
        context.get_light(Direction.SOUTH).start_green()
        context.get_light(Direction.EAST).set_color(LightColor.RED)
        context.get_light(Direction.WEST).set_color(LightColor.RED)

        # Wait for green light duration
        time.sleep(context.get_green_duration() / 1000.0)

        # Transition North and South to Yellow
        context.get_light(Direction.NORTH).transition()
        context.get_light(Direction.SOUTH).transition()

        # Wait for yellow light duration
        time.sleep(context.get_yellow_duration() / 1000.0)

        # Transition North and South to Red
        context.get_light(Direction.NORTH).transition()
        context.get_light(Direction.SOUTH).transition()

        # Change the intersection's state to let East-West go
        context.set_state(EastWestGreenState())










class GreenState(SignalState):
    def handle(self, context: 'TrafficLight'):
        context.set_color(LightColor.GREEN)
        # After being green, the next state is yellow.
        context.set_next_state(YellowState())



class RedState(SignalState):
    def handle(self, context: 'TrafficLight'):
        context.set_color(LightColor.RED)
        # Red is a stable state, it transitions to green only when the intersection controller commands it.
        # So, the next state is self.
        context.set_next_state(RedState())





class SignalState(ABC):
    @abstractmethod
    def handle(self, context: 'TrafficLight'):
        pass



class YellowState(SignalState):
    def handle(self, context: 'TrafficLight'):
        context.set_color(LightColor.YELLOW)
        # After being yellow, the next state is red.
        context.set_next_state(RedState())







class IntersectionController:
    def __init__(self, intersection_id: int, traffic_lights: Dict[Direction, TrafficLight], 
                 green_duration: int, yellow_duration: int):
        self._id = intersection_id
        self._traffic_lights = traffic_lights
        self._green_duration = green_duration
        self._yellow_duration = yellow_duration
        self._current_state = NorthSouthGreenState()  # Initial state for the intersection
        self._running = True

    def get_id(self) -> int:
        return self._id

    def get_green_duration(self) -> int:
        return self._green_duration

    def get_yellow_duration(self) -> int:
        return self._yellow_duration

    def get_light(self, direction: Direction) -> TrafficLight:
        return self._traffic_lights[direction]

    def set_state(self, state: IntersectionState):
        self._current_state = state

    def start(self):
        thread = threading.Thread(target=self.run)
        thread.start()

    def stop(self):
        self._running = False

    def run(self):
        while self._running:
            try:
                self._current_state.handle(self)
            except Exception as e:
                print(f"Intersection {self._id} encountered an error: {e}")
                self._running = False

    # Builder Pattern
    class Builder:
        def __init__(self, intersection_id: int):
            self._id = intersection_id
            self._green_duration = 5000  # default 5s
            self._yellow_duration = 2000  # default 2s
            self._observers: List[TrafficObserver] = []

        def with_durations(self, green: int, yellow: int) -> 'IntersectionController.Builder':
            self._green_duration = green
            self._yellow_duration = yellow
            return self

        def add_observer(self, observer: TrafficObserver) -> 'IntersectionController.Builder':
            self._observers.append(observer)
            return self

        def build(self) -> 'IntersectionController':
            lights = {}
            for direction in Direction:
                light = TrafficLight(self._id, direction)
                # Attach all registered observers to each light
                for observer in self._observers:
                    light.add_observer(observer)
                lights[direction] = light
            return IntersectionController(self._id, lights, self._green_duration, self._yellow_duration)








class TrafficControlSystem:
    _instance = None
    _lock = threading.Lock()

    def __new__(cls):
        if cls._instance is None:
            with cls._lock:
                if cls._instance is None:
                    cls._instance = super().__new__(cls)
                    cls._instance._initialized = False
        return cls._instance

    def __init__(self):
        if not self._initialized:
            self._intersections: List[IntersectionController] = []
            self._executor_service = None
            self._initialized = True

    @classmethod
    def get_instance(cls):
        return cls()

    def add_intersection(self, intersection_id: int, green_duration: int, yellow_duration: int):
        intersection = IntersectionController.Builder(intersection_id) \
            .with_durations(green_duration, yellow_duration) \
            .add_observer(CentralMonitor()) \
            .build()
        self._intersections.append(intersection)

    def start_system(self):
        if not self._intersections:
            print("No intersections to manage. System not starting.")
            return

        print("--- Starting Traffic Control System ---")
        self._executor_service = ThreadPoolExecutor(max_workers=len(self._intersections))
        
        for intersection in self._intersections:
            self._executor_service.submit(intersection.run)

    def stop_system(self):
        print("\n--- Shutting Down Traffic Control System ---")
        
        for intersection in self._intersections:
            intersection.stop()
        
        if self._executor_service:
            self._executor_service.shutdown(wait=True)
        
        print("All intersections stopped. System shut down.")







class TrafficLight:
    def __init__(self, intersection_id: int, direction: Direction):
        self._intersection_id = intersection_id
        self._direction = direction
        self._current_color = None
        self._current_state = RedState()  # Default state is Red
        self._next_state = None
        self._observers: List[TrafficObserver] = []
        self._current_state.handle(self)

    # This is called by the IntersectionController to initiate a G-Y-R cycle
    def start_green(self):
        self._current_state = GreenState()
        self._current_state.handle(self)

    # This is called by the IntersectionController to transition from G->Y or Y->R
    def transition(self):
        self._current_state = self._next_state
        self._current_state.handle(self)

    def set_color(self, color: LightColor):
        if self._current_color != color:
            self._current_color = color
            self._notify_observers()

    def set_next_state(self, state: SignalState):
        self._next_state = state

    def get_current_color(self) -> LightColor:
        return self._current_color

    def get_direction(self) -> Direction:
        return self._direction

    # Observer pattern methods
    def add_observer(self, observer: TrafficObserver):
        self._observers.append(observer)

    def remove_observer(self, observer: TrafficObserver):
        if observer in self._observers:
            self._observers.remove(observer)

    def _notify_observers(self):
        for observer in self._observers:
            observer.update(self._intersection_id, self._direction, self._current_color)








class TrafficSystemDemo:
    @staticmethod
    def main():
        # 1. Get the singleton TrafficControlSystem instance
        system = TrafficControlSystem.get_instance()

        # 2. Add intersections to the system
        system.add_intersection(1, 500, 200)
        system.add_intersection(2, 700, 150)

        # 3. Start the system
        system.start_system()

        # 4. Let the simulation run for a while (e.g., 5 seconds)
        try:
            time.sleep(5)
        except KeyboardInterrupt:
            pass

        # 5. Stop the system gracefully
        system.stop_system()

if __name__ == "__main__":
    TrafficSystemDemo.main()










































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































