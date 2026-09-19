extends Control

var stars: Array[Vector2] = []
var t := 0.0

func _ready() -> void:
    mouse_filter = Control.MOUSE_FILTER_IGNORE
    var rng := RandomNumberGenerator.new()
    rng.seed = 20260919
    for i in range(90):
        stars.append(Vector2(rng.randf(), rng.randf()))
    set_process(true)

func _process(delta: float) -> void:
    t += delta
    queue_redraw()

func _draw() -> void:
    if size.x <= 1 or size.y <= 1:
        return
    var bands := 30
    for i in range(bands):
        var f := float(i) / float(bands - 1)
        var c := Color(0.035, 0.07, 0.055).lerp(Color(0.11, 0.17, 0.13), f)
        draw_rect(Rect2(0, f * size.y, size.x, size.y / bands + 2), c)
    for i in range(stars.size()):
        var p := Vector2(stars[i].x * size.x, stars[i].y * size.y)
        var a := 0.15 + 0.20 * (sin(t * 0.7 + i * 1.7) * 0.5 + 0.5)
        draw_circle(p, 1.2 + float(i % 4) * 0.35, Color(1.0, 0.93, 0.68, a))
    var hill := PackedVector2Array([
        Vector2(0,size.y), Vector2(0,size.y*0.80), Vector2(size.x*.10,size.y*.74),
        Vector2(size.x*.22,size.y*.81), Vector2(size.x*.34,size.y*.71), Vector2(size.x*.48,size.y*.81),
        Vector2(size.x*.61,size.y*.73), Vector2(size.x*.74,size.y*.81), Vector2(size.x*.88,size.y*.72),
        Vector2(size.x,size.y*.78), Vector2(size.x,size.y)
    ])
    draw_colored_polygon(hill, Color(0.02,0.06,0.04,0.72))
