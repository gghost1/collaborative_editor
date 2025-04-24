import { useEffect, useRef } from 'react';
import SockJS from 'sockjs-client';
import { Client, Message } from '@stomp/stompjs';
import { useDispatch, useSelector } from 'react-redux';
import type { RootState } from '../store/store';
import { addPixels, flushPending } from '../store/pixelsSlice';
import type { Pixel } from '../entities/Pixel';
import { throttle } from 'lodash';

export const SocketHandler: React.FC<{ roomId: string }> = ({ roomId }) => {
  const dispatch = useDispatch();
  const clientRef = useRef<Client | null>(null);
  const pendingPixels = useSelector((s: RootState) => s.pixels.pending);
  const clientId = useRef(Math.random().toString(36).substr(2, 9));

  const sendThrottled = useRef(
      throttle((pixels: Pixel[]) => {
        if (!clientRef.current || pixels.length === 0) return;

        const batch = deduplicatePixels(pixels);
        const payload = {
          value: batch,
          senderId: clientId.current,
          chunkId: Date.now().toString()
        };

        clientRef.current.publish({
          destination: `/api/draw/${roomId}`,
          headers: { 'content-type': 'application/json' },
          body: JSON.stringify(payload),
        });
        dispatch(flushPending());
      }, 30)
  ).current;

  const deduplicatePixels = (pixels: Pixel[]): Pixel[] => {
    const unique = new Map<string, Pixel>();
    pixels.forEach(p => unique.set(`${p.x},${p.y}-${p.color}`, p));
    return Array.from(unique.values());
  };

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str: string) => console.debug('[STOMP]', str),
      onConnect: () => {
        client.subscribe(`/canvas/${roomId}`, (message: Message) => {
          const { pixels, senderId } = JSON.parse(message.body);
          if (senderId === clientId.current) return;
          dispatch(addPixels(pixels));
        });
      },
    });

    client.activate();
    clientRef.current = client;

    return () => {
      if (client.connected) client.deactivate();
    };
  }, [dispatch, roomId]);

  useEffect(() => {
    if (pendingPixels.length > 0) sendThrottled(pendingPixels);
  }, [pendingPixels, sendThrottled]);

  return null;
};