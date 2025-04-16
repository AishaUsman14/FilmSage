# FilmSage

FilmSage is a movie recommendation chatbot designed to help users discover movies based on their preferences. With a simple and intuitive interface, FilmSage offers personalized recommendations, a watchlist, and keeps track of previous conversations through a chat history feature.

## Features

- **Movie Discovery**: Search for movies based on various criteria such as genre, ratings, and popularity.
- **Watchlist Management**: Save your favorite movie recommendations to a personalized watchlist for easy access later.
- **Chat History**: Keeps track of previous conversations and displays a history of movie recommendations.
- **Personalized Recommendations**: Suggests movies based on your previous preferences and mood.
  
## Tech Stack

This project uses the following technologies:

- **Java**: Backend logic and API development.
- **Spring Boot**: Backend framework for building the RESTful API.
- **HTML**: Structuring the web pages.
- **CSS**: Styling the user interface for a clean and responsive design.
- **JavaScript**: Dynamic front-end behavior and handling user interactions.
- **MySQL**: Database for storing user data, movie recommendations, and watchlist.
- **HTMX & Thymeleaf**: For dynamic content rendering on the front-end.

## Getting Started

### Prerequisites

To run this project locally, make sure you have the following installed:

- **Java Development Kit (JDK)** (version 17 or above).
- **Spring Boot** for backend development.
- A **MySQL** database.
- A **web browser** to access the front-end.
- **IDE** such as IntelliJ IDEA, VS Code, or Eclipse for editing and running the code.

### Installation

1. **Clone the repository**:

   ```bash
   git clone https://github.com/AishaUsman14/FilmSage.git
   cd FilmSage
Set up MySQL Database:

Create a new database named filmsage in your MySQL server.

Set up tables for storing movie recommendations, user data, and the watchlist.

Build and run the backend:

In the root directory of the project, use Maven to build and run the Spring Boot application:

bash
Copy
Edit
mvn clean install
mvn spring-boot:run
Open the front-end:

Navigate to the src/main/resources/static folder and open the index.html file in your browser.

Usage
Once everything is set up:

Start a conversation: Interact with the chatbot to get movie recommendations.

Save to Watchlist: Add movies to your watchlist by clicking the "Save" button next to each recommendation.

View Chat History: Review previous movie recommendations and saved conversations.

Chat History Feature
The chatbot stores all conversations, allowing you to see previous recommendations and movie details you’ve interacted with.

The history is accessible on the left side of the interface, and you can refer to past recommendations.

Contributing
Contributions to the FilmSage project are welcome! If you’d like to contribute, please follow these steps:

Fork the repository.

Create a new feature branch:

bash
Copy
Edit
git checkout -b feature-name
Commit your changes:

bash
Copy
Edit
git commit -m 'Add new feature'
Push your changes:

bash
Copy
Edit
git push origin feature-name
Open a pull request on GitHub.

License
This project is licensed under the MIT License. See the LICENSE file for details.

Acknowledgements
The FilmSage team for their hard work and dedication.

Open-source libraries and tools used in this project.

Spring Boot for making backend development easier.

MySQL for the reliable database storage solution.

Contact
For any inquiries or feedback, feel free to reach out to me:

GitHub: AishaUsman14

Email: ayeeshausman60@gmail.com
