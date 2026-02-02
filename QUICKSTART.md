# Chess App - Quick Start Guide

## ✅ What's Implemented

### Backend Features
- ✅ User authentication (Google OAuth2)
- ✅ JWT-based session management
- ✅ Matchmaking system (automatic pairing)
- ✅ Real-time WebSocket game communication
- ✅ Move validation and game logic
- ✅ Game state management
- ✅ Resign/draw functionality

### Frontend Features
- ✅ User login/signup
- ✅ Matchmaking UI with search timer
- ✅ Real-time game board
- ✅ Move history tracking
- ✅ WebSocket integration
- ✅ Player information display
- ✅ Captured pieces tracking
- ✅ Game controls (resign, draw offer)

## 🚀 How to Start the App

### 1. Start Backend (IntelliJ or Terminal)

**Option A: IntelliJ IDEA**
1. Open `IndiChessBackend` folder in IntelliJ
2. Right-click `IndiChessBackendApplication.java`
3. Select "Run" or "Debug"

**Option B: Terminal/PowerShell**
```powershell
cd "X:\Chess App\IndiChessBackend"
.\mvnw.cmd spring-boot:run
```

✅ Backend runs on: **http://localhost:8080**

### 2. Start Frontend

Open a **new** PowerShell window:
```powershell
cd "X:\Chess App\indichessfrontend"
npm start
```

✅ Frontend runs on: **http://localhost:3000**

## 🎮 How to Play

### Step 1: Login
1. Go to http://localhost:3000
2. Click "Sign in with Google" or create an account
3. After login, you'll be redirected to the Home page

### Step 2: Start a Game
On the Home page, you have two options:

**Option A: From Home Page**
- Click the **"New Game"** button in the GameInfo panel (left side)
- Wait for matchmaking (search indicator shows elapsed time)
- Another player must also click "New Game" to be matched

**Option B: From New Game Component**
- Click "Play" in the side navigation (if available)
- Click **"🎮 Find Opponent"** button
- Wait for another player

### Step 3: Playing
Once matched:
- The game board loads automatically at `/game/{matchId}`
- Click a piece to see valid moves (highlighted squares)
- Click a destination square to move
- Moves are sent via WebSocket to your opponent in real-time
- Move history appears on the right side
- Use "Resign" or "Draw Offer" buttons as needed

### Step 4: Testing with Two Players

**To test locally, open TWO browser windows:**

1. **Browser Window 1**
   - Go to http://localhost:3000
   - Login with Google Account 1
   - Click "New Game" → Wait for opponent

2. **Browser Window 2** (Incognito/Private Mode)
   - Go to http://localhost:3000
   - Login with Google Account 2 (or different account)
   - Click "New Game"
   - Both players are matched!

## 🔍 Troubleshooting

### Backend won't start
- **Error: "Access denied for user 'root'"**
  - Ensure MySQL Docker container is running: `docker ps`
  - Database `indichessdb` should exist (already created)
  - Check credentials in `application.properties`

### Frontend compilation errors
- Run `npm install` to ensure all dependencies are installed
- Check browser console (F12) for runtime errors

### Matchmaking not working
- Ensure backend is running (check logs for "Tomcat started on port 8080")
- Two users must click "New Game" within polling window
- Check browser Network tab for `/game` and `/game/check-match` requests

### WebSocket not connecting
- Check browser console for WebSocket connection logs
- Backend should show: "WebSocket message broker started"
- Ensure CORS is allowing `http://localhost:3000`

## 📊 Architecture Summary

### Backend Endpoints
- `POST /game` - Create/join match (matchmaking)
- `GET /game/check-match` - Poll for match readiness
- `POST /game/cancel-waiting` - Cancel matchmaking
- `GET /api/games/{matchId}` - Get game details
- `GET /me` - Get current user

### WebSocket Topics
- **Subscribe (client receives):**
  - `/topic/moves/{matchId}` - Opponent moves
  - `/topic/game/{matchId}` - Game join events
  - `/topic/game-state/{matchId}` - Game status (resign, draw, etc.)

- **Publish (client sends):**
  - `/app/game/{matchId}/move` - Send your move
  - `/app/game/{matchId}/resign` - Resign from game
  - `/app/game/{matchId}/draw` - Offer draw

## 🎨 UI Features
- Gradient dark theme
- Animated search indicator with pulsing effect
- Real-time move updates
- Captured pieces display
- Player timers (visual only, not enforced yet)
- Move history with notation

## 🔮 Future Enhancements (Not Yet Implemented)
- Play against AI/Bots
- Play with Friends (direct invite)
- Tournaments
- Time controls enforcement
- Chat functionality
- Game replay/analysis
- Leaderboards
- User profiles with stats

---

**Need help?** Check the browser console (F12) and backend terminal logs for error details.

**Enjoy your chess game!** ♟️
