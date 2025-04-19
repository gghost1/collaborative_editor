import React, { useRef, useEffect, useState } from 'react';
import type { Pixel } from '../entities/Pixel';

const CollaborativeCanvas: React.FC = () => {

  const canvasRef = useRef<HTMLCanvasElement>(null);
  const isDrawing = useRef<boolean>(false);
  const lastPosRef = useRef<{ x: number; y: number }>({ x: 0, y: 0 });
  const [pixelLogs, setPixelLogs] = useState<Pixel[][]>([]);

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

    let currentX = x0;
    let currentY = y0;
    while (true) {
      pixels.push({ x: currentX, y: currentY, color: strokeStyle });
      if (currentX === x1 && currentY === y1) break;
      const e2 = 2 * err;
      if (e2 > -dy) {
        err -= dy;
        currentX += sx;
      }
      if (e2 < dx) {
        err += dx;
        currentY += sy;
      }
    }
    return pixels;
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.lineJoin = 'round';
    ctx.lineCap = 'round';
    ctx.lineWidth = lineWidth;
    ctx.strokeStyle = strokeStyle;

    const handleMouseDown = (e: MouseEvent) => {
      isDrawing.current = true;
      const pos = getMousePos(canvas, e);
      lastPosRef.current = pos;
    };

    const handleMouseMove = (e: MouseEvent) => {
      if (!isDrawing.current) return;
      const pos = getMousePos(canvas, e);

      ctx.beginPath();
      ctx.moveTo(lastPosRef.current.x, lastPosRef.current.y);
      ctx.lineTo(pos.x, pos.y);
      ctx.stroke();

      const changedPixels = getLinePixels(
        lastPosRef.current.x,
        lastPosRef.current.y,
        pos.x,
        pos.y
      );
      console.log('Changed pixels:', changedPixels);
      setPixelLogs((prevLogs) => [...prevLogs, changedPixels]);

      lastPosRef.current = pos;
    };

    const handleMouseUpOrLeave = () => {
      isDrawing.current = false;
    };

    canvas.addEventListener('mousedown', handleMouseDown);
    canvas.addEventListener('mousemove', handleMouseMove);
    canvas.addEventListener('mouseleave', handleMouseUpOrLeave);
    window.addEventListener('mouseup', handleMouseUpOrLeave);

    return () => {
      canvas.removeEventListener('mousedown', handleMouseDown);
      canvas.removeEventListener('mousemove', handleMouseMove);
      canvas.removeEventListener('mouseleave', handleMouseUpOrLeave);
      window.removeEventListener('mouseup', handleMouseUpOrLeave);
    };
  }, []);

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
          {JSON.stringify(pixelLogs, null, 2)}
        </pre>
      </div>
    </div>
  );
};

export default CollaborativeCanvas;
