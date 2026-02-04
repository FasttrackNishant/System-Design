enum PaymentStatus
{
    SUCCESS,
    FAILURE,
    PENDING
}





enum SeatStatus
{
    AVAILABLE,
    BOOKED,
    LOCKED // Temporarily held during booking process
}





enum SeatType
{
    REGULAR,
    PREMIUM,
    RECLINER
}

static class SeatTypeExtensions
{
    public static double GetPrice(this SeatType seatType)
    {
        switch (seatType)
        {
            case SeatType.REGULAR: return 50.0;
            case SeatType.PREMIUM: return 80.0;
            case SeatType.RECLINER: return 120.0;
            default: return 50.0;
        }
    }
}









class Booking
{
    private readonly string id;
    private readonly User user;
    private readonly Show show;
    private readonly List<Seat> seats;
    private readonly double totalAmount;
    private readonly Payment payment;

    public Booking(string id, User user, Show show, List<Seat> seats, double totalAmount, Payment payment)
    {
        this.id = id;
        this.user = user;
        this.show = show;
        this.seats = seats;
        this.totalAmount = totalAmount;
        this.payment = payment;
    }

    public void ConfirmBooking()
    {
        foreach (var seat in seats)
        {
            seat.SetStatus(SeatStatus.BOOKED);
        }
    }

    public string GetId() { return id; }
    public User GetUser() { return user; }
    public Show GetShow() { return show; }
    public List<Seat> GetSeats() { return seats; }
    public double GetTotalAmount() { return totalAmount; }
    public Payment GetPayment() { return payment; }
}

class BookingBuilder
{
    private string id;
    private User user;
    private Show show;
    private List<Seat> seats;
    private double totalAmount;
    private Payment payment;

    public BookingBuilder SetId(string id)
    {
        this.id = id;
        return this;
    }

    public BookingBuilder SetUser(User user)
    {
        this.user = user;
        return this;
    }

    public BookingBuilder SetShow(Show show)
    {
        this.show = show;
        return this;
    }

    public BookingBuilder SetSeats(List<Seat> seats)
    {
        this.seats = seats;
        return this;
    }

    public BookingBuilder SetTotalAmount(double totalAmount)
    {
        this.totalAmount = totalAmount;
        return this;
    }

    public BookingBuilder SetPayment(Payment payment)
    {
        this.payment = payment;
        return this;
    }

    public Booking Build()
    {
        // Validations can be added here
        return new Booking(id ?? Guid.NewGuid().ToString(), user, show, seats, totalAmount, payment);
    }
}










class Cinema
{
    private readonly string id;
    private readonly string name;
    private readonly City city;
    private readonly List<Screen> screens;

    public Cinema(string id, string name, City city, List<Screen> screens)
    {
        this.id = id;
        this.name = name;
        this.city = city;
        this.screens = screens;
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
    public City GetCity() { return city; }
    public List<Screen> GetScreens() { return screens; }
}








class City
{
    private readonly string id;
    private readonly string name;

    public City(string id, string name)
    {
        this.id = id;
        this.name = name;
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
}







class Movie : MovieSubject
{
    private readonly string id;
    private readonly string title;
    private readonly int durationInMinutes;

    public Movie(string id, string title, int durationInMinutes)
    {
        this.id = id;
        this.title = title;
        this.durationInMinutes = durationInMinutes;
    }

    public string GetId() { return id; }
    public string GetTitle() { return title; }
}





class Payment
{
    private readonly string id;
    private readonly double amount;
    private readonly PaymentStatus status;
    private readonly string transactionId;

    public Payment(double amount, PaymentStatus status, string transactionId)
    {
        this.id = Guid.NewGuid().ToString();
        this.amount = amount;
        this.status = status;
        this.transactionId = transactionId;
    }

    public PaymentStatus GetStatus() { return status; }
}







class Screen
{
    private readonly string id;
    private readonly List<Seat> seats;

    public Screen(string id)
    {
        this.id = id;
        this.seats = new List<Seat>();
    }

    public void AddSeat(Seat seat)
    {
        seats.Add(seat);
    }

    public string GetId() { return id; }
    public List<Seat> GetSeats() { return seats; }
}







class Seat
{
    private readonly string id;
    private readonly int row;
    private readonly int col;
    private readonly SeatType type;
    private SeatStatus status;

    public Seat(string id, int row, int col, SeatType type)
    {
        this.id = id;
        this.row = row;
        this.col = col;
        this.type = type;
        this.status = SeatStatus.AVAILABLE;
    }

    public string GetId() { return id; }
    public int GetRow() { return row; }
    public int GetCol() { return col; }
    public SeatType GetSeatType() { return type; }
    public SeatStatus GetStatus() { return status; }
    public void SetStatus(SeatStatus status) { this.status = status; }
}






class Show
{
    private readonly string id;
    private readonly Movie movie;
    private readonly Screen screen;
    private readonly DateTime startTime;
    private readonly IPricingStrategy pricingStrategy;

    public Show(string id, Movie movie, Screen screen, DateTime startTime, IPricingStrategy pricingStrategy)
    {
        this.id = id;
        this.movie = movie;
        this.screen = screen;
        this.startTime = startTime;
        this.pricingStrategy = pricingStrategy;
    }

    public string GetId() { return id; }
    public Movie GetMovie() { return movie; }
    public Screen GetScreen() { return screen; }
    public DateTime GetStartTime() { return startTime; }
    public IPricingStrategy GetPricingStrategy() { return pricingStrategy; }
}





class User
{
    private readonly string id;
    private readonly string name;
    private readonly string email;

    public User(string name, string email)
    {
        this.id = Guid.NewGuid().ToString();
        this.name = name;
        this.email = email;
    }

    public string GetId() { return id; }
    public string GetName() { return name; }
}















interface IMovieObserver
{
    void Update(Movie movie);
}



abstract class MovieSubject
{
    private readonly List<IMovieObserver> observers = new List<IMovieObserver>();

    public void AddObserver(IMovieObserver observer)
    {
        observers.Add(observer);
    }

    public void RemoveObserver(IMovieObserver observer)
    {
        observers.Remove(observer);
    }

    public void NotifyObservers()
    {
        foreach (var observer in observers)
        {
            observer.Update((Movie)this);
        }
    }
}



class UserObserver : IMovieObserver
{
    private readonly User user;

    public UserObserver(User user)
    {
        this.user = user;
    }

    public void Update(Movie movie)
    {
        Console.WriteLine($"Notification for {user.GetName()} ({user.GetId()}): Movie '{movie.GetTitle()}' is now available for booking!");
    }
}











class CreditCardPaymentStrategy : IPaymentStrategy
{
    private readonly string cardNumber;
    private readonly string cvv;

    public CreditCardPaymentStrategy(string cardNumber, string cvv)
    {
        this.cardNumber = cardNumber;
        this.cvv = cvv;
    }

    public Payment Pay(double amount)
    {
        Console.WriteLine($"Processing credit card payment of ${amount:F2}");
        // Simulate payment gateway interaction
        var random = new Random();
        bool paymentSuccess = random.NextDouble() > 0.05; // 95% success rate
        
        return new Payment(
            amount,
            paymentSuccess ? PaymentStatus.SUCCESS : PaymentStatus.FAILURE,
            $"TXN_{Guid.NewGuid()}"
        );
    }
}



interface IPaymentStrategy
{
    Payment Pay(double amount);
}








interface IPricingStrategy
{
    double CalculatePrice(List<Seat> seats);
}


class WeekdayPricingStrategy : IPricingStrategy
{
    public double CalculatePrice(List<Seat> seats)
    {
        return seats.Sum(seat => seat.GetSeatType().GetPrice());
    }
}



class WeekendPricingStrategy : IPricingStrategy
{
    private const double WEEKEND_SURCHARGE = 1.2; // 20% surcharge

    public double CalculatePrice(List<Seat> seats)
    {
        double basePrice = seats.Sum(seat => seat.GetSeatType().GetPrice());
        return basePrice * WEEKEND_SURCHARGE;
    }
}














class BookingManager
{
    private readonly SeatLockManager seatLockManager;

    public BookingManager(SeatLockManager seatLockManager)
    {
        this.seatLockManager = seatLockManager;
    }

    public Booking CreateBooking(User user, Show show, List<Seat> seats, IPaymentStrategy paymentStrategy)
    {
        // 1. Lock the seats
        seatLockManager.LockSeats(show, seats, user.GetId());

        // 2. Calculate the total price
        double totalAmount = show.GetPricingStrategy().CalculatePrice(seats);

        // 3. Process Payment
        Payment payment = paymentStrategy.Pay(totalAmount);

        // 4. If payment is successful, create the booking
        if (payment.GetStatus() == PaymentStatus.SUCCESS)
        {
            Booking booking = new BookingBuilder()
                .SetUser(user)
                .SetShow(show)
                .SetSeats(seats)
                .SetTotalAmount(totalAmount)
                .SetPayment(payment)
                .Build();

            // 5. Confirm the booking (mark seats as BOOKED)
            booking.ConfirmBooking();

            // Clean up the lock map
            seatLockManager.UnlockSeats(show, seats, user.GetId());

            return booking;
        }
        else
        {
            Console.WriteLine("Payment failed. Please try again.");
            return null;
        }
    }
}







using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;

public class MovieBookingDemo
{
    public static void Main(string[] args)
    {
        // Setup
        var service = MovieBookingService.GetInstance();

        var nyc = service.AddCity("city1", "New York");
        var la = service.AddCity("city2", "Los Angeles");

        // 2. Add movies
        var matrix = new Movie("M1", "The Matrix", 120);
        var avengers = new Movie("M2", "Avengers: Endgame", 170);
        service.AddMovie(matrix);
        service.AddMovie(avengers);

        // Add Seats for a Screen
        var screen1 = new Screen("S1");

        for (int i = 1; i <= 10; i++)
        {
            var seatType = i <= 5 ? SeatType.REGULAR : SeatType.PREMIUM;
            screen1.AddSeat(new Seat($"A{i}", 1, i, seatType));
            screen1.AddSeat(new Seat($"B{i}", 2, i, seatType));
        }

        // Add Cinemas
        var amcNYC = service.AddCinema("cinema1", "AMC Times Square", nyc.GetId(), new List<Screen> { screen1 });

        // Add Shows
        var matrixShow = service.AddShow("show1", matrix, screen1, DateTime.Now.AddHours(2), new WeekdayPricingStrategy());
        var avengersShow = service.AddShow("show2", avengers, screen1, DateTime.Now.AddHours(5), new WeekdayPricingStrategy());

        // --- User and Observer Setup ---
        var alice = service.CreateUser("Alice", "alice@example.com");
        var aliceObserver = new UserObserver(alice);
        avengers.AddObserver(aliceObserver);

        // Simulate movie release
        Console.WriteLine("\n--- Notifying Observers about Movie Release ---");
        avengers.NotifyObservers();

        // --- User Story: Alice books tickets ---
        Console.WriteLine("\n--- Alice's Booking Flow ---");
        string cityName = "New York";
        string movieTitle = "Avengers: Endgame";

        // 1. Search for shows
        var availableShows = service.FindShows(movieTitle, cityName);
        if (!availableShows.Any())
        {
            Console.WriteLine($"No shows found for {movieTitle} in {cityName}");
            return;
        }
        var selectedShow = availableShows[0]; // Alice selects the first show

        // 2. View available seats
        var availableSeats = selectedShow.GetScreen().GetSeats()
            .Where(seat => seat.GetStatus() == SeatStatus.AVAILABLE)
            .ToList();
        
        Console.WriteLine($"Available seats for '{selectedShow.GetMovie().GetTitle()}' at {selectedShow.GetStartTime()}: {string.Join(", ", availableSeats.Select(s => s.GetId()))}");

        // 3. Select seats
        var desiredSeats = new List<Seat> { availableSeats[2], availableSeats[3] };
        Console.WriteLine($"Alice selects seats: {string.Join(", ", desiredSeats.Select(s => s.GetId()))}");

        // 4. Book Tickets
        var booking = service.BookTickets(
            alice.GetId(),
            selectedShow.GetId(),
            desiredSeats,
            new CreditCardPaymentStrategy("1234-5678-9876-5432", "123")
        );

        if (booking != null)
        {
            Console.WriteLine("\n--- Booking Successful! ---");
            Console.WriteLine($"Booking ID: {booking.GetId()}");
            Console.WriteLine($"User: {booking.GetUser().GetName()}");
            Console.WriteLine($"Movie: {booking.GetShow().GetMovie().GetTitle()}");
            Console.WriteLine($"Seats: {string.Join(", ", booking.GetSeats().Select(s => s.GetId()))}");
            Console.WriteLine($"Total Amount: ${booking.GetTotalAmount()}");
            Console.WriteLine($"Payment Status: {booking.GetPayment().GetStatus()}");
        }
        else
        {
            Console.WriteLine("Booking failed.");
        }

        // 5. Verify seat status after booking
        Console.WriteLine("\nSeat status after Alice's booking:");
        foreach (var seat in desiredSeats)
        {
            Console.WriteLine($"Seat {seat.GetId()} status: {seat.GetStatus()}");
        }

        // 6. Shut down the system to release resources like the scheduler.
        service.Shutdown();
    }
}








class MovieBookingService
{
    private static volatile MovieBookingService instance;
    private static readonly object syncRoot = new object();

    private readonly Dictionary<string, City> cities;
    private readonly Dictionary<string, Cinema> cinemas;
    private readonly Dictionary<string, Movie> movies;
    private readonly Dictionary<string, User> users;
    private readonly Dictionary<string, Show> shows;

    private readonly SeatLockManager seatLockManager;
    private readonly BookingManager bookingManager;

    private MovieBookingService()
    {
        this.cities = new Dictionary<string, City>();
        this.cinemas = new Dictionary<string, Cinema>();
        this.movies = new Dictionary<string, Movie>();
        this.users = new Dictionary<string, User>();
        this.shows = new Dictionary<string, Show>();

        this.seatLockManager = new SeatLockManager();
        this.bookingManager = new BookingManager(seatLockManager);
    }

    public static MovieBookingService GetInstance()
    {
        if (instance == null)
        {
            lock (syncRoot)
            {
                if (instance == null)
                {
                    instance = new MovieBookingService();
                }
            }
        }
        return instance;
    }

    public BookingManager GetBookingManager()
    {
        return bookingManager;
    }

    public City AddCity(string id, string name)
    {
        var city = new City(id, name);
        cities[city.GetId()] = city;
        return city;
    }

    public Cinema AddCinema(string id, string name, string cityId, List<Screen> screens)
    {
        var city = cities[cityId];
        var cinema = new Cinema(id, name, city, screens);
        cinemas[cinema.GetId()] = cinema;
        return cinema;
    }

    public void AddMovie(Movie movie)
    {
        movies[movie.GetId()] = movie;
    }

    public Show AddShow(string id, Movie movie, Screen screen, DateTime startTime, IPricingStrategy pricingStrategy)
    {
        var show = new Show(id, movie, screen, startTime, pricingStrategy);
        shows[show.GetId()] = show;
        return show;
    }

    public User CreateUser(string name, string email)
    {
        var user = new User(name, email);
        users[user.GetId()] = user;
        return user;
    }

    public Booking BookTickets(string userId, string showId, List<Seat> desiredSeats, IPaymentStrategy paymentStrategy)
    {
        return bookingManager.CreateBooking(
            users[userId],
            shows[showId],
            desiredSeats,
            paymentStrategy
        );
    }

    public List<Show> FindShows(string movieTitle, string cityName)
    {
        var result = new List<Show>();
        foreach (var show in shows.Values)
        {
            if (show.GetMovie().GetTitle().Equals(movieTitle, StringComparison.OrdinalIgnoreCase))
            {
                var cinema = FindCinemaForShow(show);
                if (cinema != null && cinema.GetCity().GetName().Equals(cityName, StringComparison.OrdinalIgnoreCase))
                {
                    result.Add(show);
                }
            }
        }
        return result;
    }

    private Cinema FindCinemaForShow(Show show)
    {
        return cinemas.Values.FirstOrDefault(cinema => cinema.GetScreens().Contains(show.GetScreen()));
    }

    public void Shutdown()
    {
        seatLockManager.Shutdown();
        Console.WriteLine("MovieTicketBookingSystem has been shut down.");
    }
}







class SeatLockManager
{
    private readonly Dictionary<Show, Dictionary<Seat, string>> lockedSeats = new Dictionary<Show, Dictionary<Seat, string>>();
    private readonly object lockObj = new object();
    private const int LOCK_TIMEOUT_MS = 500; // 0.5 seconds

    public void LockSeats(Show show, List<Seat> seats, string userId)
    {
        lock (lockObj)
        {
            // Check if any of the requested seats are already locked or booked
            foreach (var seat in seats)
            {
                if (seat.GetStatus() != SeatStatus.AVAILABLE)
                {
                    Console.WriteLine($"Seat {seat.GetId()} is not available.");
                    return;
                }
            }

            // Lock the seats
            foreach (var seat in seats)
            {
                seat.SetStatus(SeatStatus.LOCKED);
            }

            if (!lockedSeats.ContainsKey(show))
            {
                lockedSeats[show] = new Dictionary<Seat, string>();
            }

            foreach (var seat in seats)
            {
                lockedSeats[show][seat] = userId;
            }

            // Schedule a task to unlock the seats after a timeout
            Task.Delay(LOCK_TIMEOUT_MS).ContinueWith(_ => UnlockSeats(show, seats, userId));

            Console.WriteLine($"Locked seats: {string.Join(", ", seats.Select(s => s.GetId()))} for user {userId}");
        }
    }

    public void UnlockSeats(Show show, List<Seat> seats, string userId)
    {
        lock (lockObj)
        {
            if (lockedSeats.TryGetValue(show, out var showLocks))
            {
                foreach (var seat in seats)
                {
                    if (showLocks.TryGetValue(seat, out var lockedUserId) && lockedUserId == userId)
                    {
                        showLocks.Remove(seat);
                        if (seat.GetStatus() == SeatStatus.LOCKED)
                        {
                            seat.SetStatus(SeatStatus.AVAILABLE);
                            Console.WriteLine($"Unlocked seat: {seat.GetId()} due to timeout.");
                        }
                        else
                        {
                            Console.WriteLine($"Unlocked seat: {seat.GetId()} due to booking completion.");
                        }
                    }
                }

                if (!showLocks.Any())
                {
                    lockedSeats.Remove(show);
                }
            }
        }
    }

    public void Shutdown()
    {
        Console.WriteLine("Shutting down SeatLockProvider scheduler.");
    }
}








































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































































