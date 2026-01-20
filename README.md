# Chess Application

A full-stack multiplayer chess application with real-time gameplay, user authentication, and matchmaking system.

## Features

- **Real-time Multiplayer Chess**: Play chess with opponents in real-time using WebSocket connections
- **User Authentication**: Secure authentication with JWT tokens and Google OAuth 2.0 integration
- **Matchmaking System**: Queue-based matchmaking to pair players for games
- **Game History**: Track and review past games with move-by-move analysis
- **Rating System**: Player rating system to track performance
- **Responsive UI**: Modern React-based interface with smooth gameplay experience

## Tech Stack

### Backend
- **Spring Boot 3.x** - Java framework for building the REST API
- **Spring Security** - Authentication and authorization
- **Spring WebSocket** - Real-time bidirectional communication
- **MySQL** - Relational database for persistent storage
- **JWT** - Secure token-based authentication
- **OAuth 2.0** - Google authentication integration
- **Maven** - Dependency management

### Frontend
- **React** - UI library for building interactive interfaces
- **CSS3** - Styling and animations
- **WebSocket Client** - Real-time game updates

## Prerequisites

Before running this application, ensure you have the following installed:

- **Java 17+** - [Download](https://www.oracle.com/java/technologies/downloads/)
- **Node.js 16+** - [Download](https://nodejs.org/)
- **MySQL 8.0+** - [Download](https://dev.mysql.com/downloads/)
- **Maven 3.8+** - [Download](https://maven.apache.org/download.cgi) (or use included wrapper)

## Setup Instructions

### 1. Database Setup

Create a MySQL database for the application:

```sql
CREATE DATABASE indichessdb;
```

### 2. Backend Setup

Navigate to the backend directory:

```bash
cd IndiChessBackend
```

Create a `.env` file in the `IndiChessBackend` directory with your credentials:

```env
DB_USERNAME=root
DB_PASSWORD=your_database_password
GOOGLE_CLIENT_ID=your_google_client_id
GOOGLE_CLIENT_SECRET=your_google_client_secret
```

**Getting Google OAuth Credentials:**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable Google+ API
4. Go to "Credentials" → "Create Credentials" → "OAuth 2.0 Client ID"
5. Add authorized redirect URI: `http://localhost:8080/login/oauth2/code/google`
6. Copy the Client ID and Client Secret to your `.env` file

Install dependencies and run the backend:

```bash
# Using Maven wrapper (recommended)
./mvnw clean install
./mvnw spring-boot:run

# Or using Maven directly
mvn clean install
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### 3. Frontend Setup

Navigate to the frontend directory:

```bash
cd indichessfrontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm start
```

The frontend will start on `http://localhost:3000`

## Environment Variables

### Backend (.env)

| Variable | Description | Required |
|----------|-------------|----------|
| `DB_USERNAME` | MySQL database username | Yes |
| `DB_PASSWORD` | MySQL database password | Yes |
| `GOOGLE_CLIENT_ID` | Google OAuth 2.0 Client ID | Yes |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 2.0 Client Secret | Yes |

## API Endpoints

### Authentication
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login with credentials
- `GET /oauth2/authorization/google` - Google OAuth login

### User Management
- `GET /api/users/profile` - Get user profile
- `PUT /api/users/profile` - Update user profile

### Game Management
- `POST /api/match/create` - Create a new game
- `POST /api/match/join` - Join existing game
- `GET /api/match/{id}` - Get game details
- `GET /api/game/history` - Get user's game history

### WebSocket
- `/ws/game` - WebSocket endpoint for real-time game updates

## Project Structure

```
Chess/
├── IndiChessBackend/           # Spring Boot backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/example/IndiChessBackend/
│   │   │   │       ├── config/           # Security, WebSocket configs
│   │   │   │       ├── controller/       # REST controllers
│   │   │   │       ├── filters/          # JWT filters
│   │   │   │       ├── model/            # Entity models & DTOs
│   │   │   │       ├── oauth/            # OAuth handlers
│   │   │   │       ├── repo/             # JPA repositories
│   │   │   │       └── service/          # Business logic
│   │   │   └── resources/
│   │   │       └── application.properties
│   │   └── test/
│   ├── .env                     # Environment variables (not in git)
│   ├── .env.example            # Example environment file
│   └── pom.xml                 # Maven configuration
│
├── indichessfrontend/          # React frontend
│   ├── public/
│   ├── src/
│   │   ├── components/         # Reusable components
│   │   │   ├── game-page-components/   # Game-specific components
│   │   │   └── component-styles/       # Component CSS
│   │   ├── pages/              # Page components
│   │   ├── App.js              # Main app component
│   │   └── index.js            # Entry point
│   └── package.json
│
└── README.md                   # This file
```

## Running in Production

### Backend

Build the application:

```bash
cd IndiChessBackend
./mvnw clean package
```

Run the JAR file:

```bash
java -jar target/IndiChessBackend-0.0.1-SNAPSHOT.jar
```

### Frontend

Build for production:

```bash
cd indichessfrontend
npm run build
```

The optimized build will be in the `build/` directory. Deploy it to any static hosting service (Netlify, Vercel, AWS S3, etc.)

## Game Rules

The application implements standard chess rules including:
- Standard piece movements (King, Queen, Rook, Bishop, Knight, Pawn)
- Special moves: Castling, En Passant, Pawn Promotion
- Check and Checkmate detection
- Turn-based gameplay

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## Security Notes

- Never commit the `.env` file to version control
- Keep your OAuth credentials secure
- Use strong database passwords
- In production, use HTTPS for all communications
- Configure CORS appropriately for your domain

## License

This project is licensed under the MIT License.

## Troubleshooting

### Database Connection Issues
- Ensure MySQL is running
- Verify database credentials in `.env`
- Check if the database `indichessdb` exists

### WebSocket Connection Fails
- Ensure backend is running on port 8080
- Check CORS configuration in `SecurityConfig.java`
- Verify firewall settings

### OAuth Login Not Working
- Verify Google OAuth credentials
- Check redirect URI matches Google Console settings
- Ensure Google+ API is enabled

## Contact

For issues and questions, please open an issue on GitHub.

---

**Happy Gaming! ♟️**
