import React, { useRef, useEffect, useCallback, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { addPixels } from '../store/pixelsSlice';
import type { RootState } from '../store/store';
import type { Pixel } from '../entities/Pixel';
import { throttle } from 'lodash';
import { useParams } from 'react-router-dom';

const Canvas: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const isDrawing = useRef(false);
  const lastPosRef = useRef<{ x: number; y: number } | null>(null);
  const pendingChunks = useRef<Pixel[][]>([]);
  const [currentColor] = useState('#000000');
  const { roomId: canvasId } = useParams<{ roomId: string }>();

  const dispatch = useDispatch();
  const { rendered } = useSelector((s: RootState) => s.pixels);

  useEffect(() => {
    fetch(`http://localhost:8080/${canvasId}`)
      .then(res => {
        console.log(res)
        if (!res.ok) throw new Error(`HTTP error! status: ${res.status}`);
        return res.json();                       // Fetch API → JSON :contentReference[oaicite:2]{index=2}
      })                           // MDN: fetch → Promise → JSON :contentReference[oaicite:1]{index=1}
      .then((data: { pixels: Pixel[] }) => {
        dispatch(addPixels(data.pixels));                   // SO: как диспатчить экшен внутри useEffect :contentReference[oaicite:2]{index=2}
      })
      .catch(err => console.error("Fetch error:", err));
  }, [canvasId, dispatch]);

  const sendChunk = useRef(
      throttle((chunk: Pixel[]) => {
        if (chunk.length > 0) dispatch(addPixels(chunk));
      }, 30)
  ).current;

  const drawBuffer = useCallback(() => {
    const ctx = canvasRef.current?.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, 1200, 600);

    rendered.forEach(p => {
      ctx.fillStyle = p.color;
      ctx.fillRect(p.x, p.y, 1, 1);
    });

    pendingChunks.current.forEach(chunk => {
      chunk.forEach(p => {
        ctx.fillStyle = p.color;
        ctx.fillRect(p.x, p.y, 1, 1);
      });
    });

    requestAnimationFrame(drawBuffer);
  }, [rendered]);

  useEffect(() => { drawBuffer(); }, [drawBuffer]);

  const getLinePixels = (x0: number, y0: number, x1: number, y1: number): Pixel[] => {
    const pixels: Pixel[] = [];
    const dx = Math.abs(x1 - x0);
    const dy = Math.abs(y1 - y0);
    const sx = x0 < x1 ? 1 : -1;
    const sy = y0 < y1 ? 1 : -1;
    let err = dx - dy;

    while (true) {
      pixels.push({ x: x0, y: y0, color: currentColor });
      if (x0 === x1 && y0 === y1) break;
      const e2 = 2 * err;
      if (e2 > -dy) { err -= dy; x0 += sx; }
      if (e2 < dx) { err += dx; y0 += sy; }
      if (pixels.length >= 500) break;
    }
    return pixels;
  };

  const handleDraw = useCallback((start: { x: number; y: number }, end: { x: number; y: number }) => {
    const pixels = getLinePixels(start.x, start.y, end.x, end.y);
    const CHUNK_SIZE = 100;

    for (let i = 0; i < pixels.length; i += CHUNK_SIZE) {
      const chunk = pixels.slice(i, i + CHUNK_SIZE);
      pendingChunks.current.push(chunk);
      sendChunk(chunk);
    }
  }, [currentColor, sendChunk]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const getPosition = (e: MouseEvent) => ({
      x: Math.floor(e.offsetX * (canvas.width / canvas.offsetWidth)),
      y: Math.floor(e.offsetY * (canvas.height / canvas.offsetHeight))
    });

    const startDrawing = (e: MouseEvent) => {
      isDrawing.current = true;
      lastPosRef.current = getPosition(e);
    };

    const draw = (e: MouseEvent) => {
      if (!isDrawing.current || !lastPosRef.current) return;
      const pos = getPosition(e);
      handleDraw(lastPosRef.current, pos);
      lastPosRef.current = pos;
    };

    const endDrawing = () => {
      isDrawing.current = false;
      pendingChunks.current = [];
    };

    canvas.addEventListener('mousedown', startDrawing);
    canvas.addEventListener('mousemove', draw);
    canvas.addEventListener('mouseup', endDrawing);
    canvas.addEventListener('mouseleave', endDrawing);

    return () => {
      canvas.removeEventListener('mousedown', startDrawing);
      canvas.removeEventListener('mousemove', draw);
      canvas.removeEventListener('mouseup', endDrawing);
      canvas.removeEventListener('mouseleave', endDrawing);
    };
  }, [handleDraw]);

  return (
      <div>
        <canvas
            ref={canvasRef}
            width={1200}
            height={600}
            style={{
              border: '1px solid #000',
              cursor: 'crosshair',
              touchAction: 'none'
            }}
        />
      </div>
  );
};

export default Canvas;