import { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { addPixels } from '../store/pixelsSlice';
import type { Pixel } from '../entities/Pixel';

export const SocketHandler: React.FC<{ roomId: string }> = ({ roomId }) => {
  const dispatch = useDispatch();
  const stompRef = useRef<any>(null);
  const localLogs = useSelector((s: RootState) => s.pixels);

  useEffect(() => {
    // Если фронт и бэк на разных портах, указываем полный URL
    const socket = new SockJS('http://localhost:8080/ws');
    const client = Stomp.over(socket);

    client.connect({}, () => {
      // Подписываемся на исходящие обновления
      // broker prefix = "/canvas"
      client.subscribe(
        `/canvas/${roomId}`,
        (msg) => {
          const remotePixels: Pixel[] = JSON.parse(msg.body);
          dispatch(addPixels(remotePixels));
        }
      );
    });

    stompRef.current = client;
    return () => { client.disconnect(); };
  }, [dispatch, roomId]);

  useEffect(() => {
    if (!stompRef.current || localLogs.length === 0) return;
    const last = localLogs[localLogs.length - 1];
    // Отправляем на контроллер handleDraw — mapping prefix = "/api", path = "/draw/{canvasId}"
    stompRef.current.send(
      `/api/draw/${roomId}`,  // <-- сюда приходит @MessageMapping("/draw/{canvasId}")
      {},
      JSON.stringify(last)
    );
  }, [localLogs, roomId]);

  return null;
};
