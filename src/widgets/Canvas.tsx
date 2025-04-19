import React, { useRef, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { addPixels } from '../store/pixelsSlice';
import type { RootState } from '../store/store';
import type { Pixel } from '../entities/Pixel';

const CollaborativeCanvas: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const isDrawing = useRef(false);
  const lastPosRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });

  const dispatch = useDispatch();
  const logs = useSelector((s: RootState) => s.pixels);

  const strokeStyle = '#000000';
  const lineWidth = 3;

  const getMousePos = (
    canvas: HTMLCanvasElement,
    evt: MouseEvent
  ): { x: number; y: number } => {
    const rect = canvas.getBoundingClientRect();
    return {
      x: Math.floor(((evt.clientX - rect.left) * canvas.width) / rect.width),
      y: Math.floor(((evt.clientY - rect.top) * canvas.height) / rect.height),
    };
  };

  const getLinePixels = (
    x0: number,
    y0: number,
    x1: number,
    y1: number
  ): Pixel[] => {
    const pixels: Pixel[] = [];
    const dx = Math.abs(x1 - x0);
    const dy = Math.abs(y1 - y0);
    const sx = x0 < x1 ? 1 : -1;
    const sy = y0 < y1 ? 1 : -1;
    let err = dx - dy;
    let cx = x0;
    let cy = y0;

    while (true) {
      pixels.push({ x: cx, y: cy, color: strokeStyle });
      if (cx === x1 && cy === y1) break;
      const e2 = 2 * err;
      if (e2 > -dy) { err -= dy; cx += sx; }
      if (e2 < dx) { err += dx; cy += sy; }
    }
    return pixels;
  };

  // Перерисовываем весь canvas при любом обновлении логов (для Undo/Replay)
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d')!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    for (const chunk of logs) {
      for (const p of chunk) {
        ctx.fillStyle = p.color;
        ctx.fillRect(p.x, p.y, 1, 1);
      }
    }
  }, [logs]);

  // Основная логика рисования
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d')!;
    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.lineWidth = lineWidth;
    ctx.strokeStyle = strokeStyle;

    const handleMouseDown = (e: MouseEvent) => {
      isDrawing.current = true;
      lastPosRef.current = getMousePos(canvas, e);
    };

    const handleMouseMove = (e: MouseEvent) => {
      if (!isDrawing.current) return;
      const pos = getMousePos(canvas, e);

      // Рисуем линию на локальном canvas
      ctx.beginPath();
      ctx.moveTo(lastPosRef.current.x, lastPosRef.current.y);
      ctx.lineTo(pos.x, pos.y);
      ctx.stroke();

      // Вычисляем пиксели и диспатчим в Redux
      const changedPixels = getLinePixels(
        lastPosRef.current.x,
        lastPosRef.current.y,
        pos.x,
        pos.y
      );
      dispatch(addPixels(changedPixels));

      lastPosRef.current = pos;
    };

    const handleMouseUpOrLeave = () => {
      isDrawing.current = false;
    };

    canvas.addEventListener('mousedown', handleMouseDown);
    canvas.addEventListener('mousemove', handleMouseMove);
    canvas.addEventListener('mouseup', handleMouseUpOrLeave);
    canvas.addEventListener('mouseleave', handleMouseUpOrLeave);

    return () => {
      canvas.removeEventListener('mousedown', handleMouseDown);
      canvas.removeEventListener('mousemove', handleMouseMove);
      canvas.removeEventListener('mouseup', handleMouseUpOrLeave);
      canvas.removeEventListener('mouseleave', handleMouseUpOrLeave);
    };
  }, [dispatch]);

  return (
    <div>
      <canvas
        ref={canvasRef}
        width={1200}
        height={600}
        style={{ border: '1px solid #000', display: 'block' }}
      />
      <div style={{ marginTop: '1em' }}>
        <h3>Лог изменённых пикселей</h3>
        <pre
          style={{
            maxHeight: '300px',
            overflow: 'auto',
            background: '#f0f0f0',
            padding: '10px',
          }}
        >
          {JSON.stringify(logs, null, 2)}
        </pre>
      </div>
    </div>
  );
};

export default CollaborativeCanvas;
