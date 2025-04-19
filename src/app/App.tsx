import React, { useState } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate, useParams } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from '../store/store';
import { SocketHandler } from '../features/SocketHandler';
import Canvas from '../widgets/Canvas';

const Home: React.FC = () => {
  const navigate = useNavigate();
  const [joinId, setJoinId] = useState('');

  const createRoom = () => {
    const roomId = Math.random().toString(36).substring(2, 8);
    navigate(`/${roomId}`);
  };

  const joinRoom = () => {
    if (joinId.trim()) {
      navigate(`/${joinId.trim()}`);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginTop: '100px' }}>
      <h1>Collaborative Canvas</h1>
      <button onClick={createRoom} style={{ padding: '10px 20px', fontSize: '16px', margin: '10px' }}>
        Create Room
      </button>
      <div style={{ display: 'flex', alignItems: 'center', marginTop: '20px' }}>
        <input
          type="text"
          placeholder="Enter Room ID"
          value={joinId}
          onChange={e => setJoinId(e.target.value)}
          style={{ padding: '8px', fontSize: '16px', marginRight: '10px' }}
        />
        <button onClick={joinRoom} style={{ padding: '10px 20px', fontSize: '16px' }}>
          Join Room
        </button>
      </div>
    </div>
  );
};

const RoomPage: React.FC = () => {
  const { roomId } = useParams<{ roomId: string }>();
  return (
    <>
      {roomId && <SocketHandler roomId={roomId} />}
      <Canvas />
    </>
  );
};

const App: React.FC = () => (
  <Provider store={store}>
    <Router>
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/:roomId" element={<RoomPage />} />
      </Routes>
    </Router>
  </Provider>
);

export default App;
