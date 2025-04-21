def merge_pixels(pixels1, pixels2):
    merged = {(p['x'], p['y']): p['color'] for p in pixels1}

    for p in pixels2: merged[(p['x'], p['y'])] = p['color']

    result = [{'x': x, 'y': y, 'color': color} for (x, y), color in merged.items()]
    return result
