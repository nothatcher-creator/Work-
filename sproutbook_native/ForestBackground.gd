extends Control

var stars: Array[Vector2] = []

func _ready() -> void:
    mouse_filter = Control.MOUSE_FILTER_IGNORE
    var rng := RandomNumberGenerator.new()
    rng.seed = 20260919
    for i in range(54):
        stars.append(Vector2(rng.randf(), rng.randf()))
    queue_redraw()

func _notification(what: int) -> void:
    if what == NOTIFICATION_RESIZED:
        queue_redraw()

func _draw() -> void:
    if size.x <= 1 or size.y <= 1:
        return

    # Static background: no per-frame redraw. This is intentionally cheap on Android.
    var bands := 14
    for i in range(bands):
        var f := float(i) / float(bands - 1)
        var c := Color("#07140f").lerp(Color("#183026"), f)
        draw_rect(Rect2(0, f * size.y, size.x, size.y / bands + 2), c)

    # Soft moon glow / ambient light.
    var glow_center := Vector2(size.x * 0.78, size.y * 0.16)
    draw_circle(glow_center, min(size.x, size.y) * 0.13, Color(0.73,0.79,0.63,0.025))
    draw_circle(glow_center, min(size.x, size.y) * 0.075, Color(0.87,0.85,0.67,0.035))

    for i in range(stars.size()):
        var p := Vector2(stars[i].x * size.x, stars[i].y * size.y)
        var a := 0.11 + float(i % 5) * 0.025
        var r := 0.9 + float(i % 3) * 0.45
        draw_circle(p, r, Color(1.0, 0.93, 0.71, a))

    # Layered forest silhouettes.
    var far_hill := PackedVector2Array([
        Vector2(0,size.y), Vector2(0,size.y*0.81), Vector2(size.x*.09,size.y*.75),
        Vector2(size.x*.20,size.y*.82), Vector2(size.x*.31,size.y*.73), Vector2(size.x*.43,size.y*.81),
        Vector2(size.x*.56,size.y*.74), Vector2(size.x*.68,size.y*.82), Vector2(size.x*.82,size.y*.72),
        Vector2(size.x,size.y*.79), Vector2(size.x,size.y)
    ])
    draw_colored_polygon(far_hill, Color(0.02,0.075,0.05,0.64))

    var near_hill := PackedVector2Array([
        Vector2(0,size.y), Vector2(0,size.y*.89), Vector2(size.x*.12,size.y*.82),
        Vector2(size.x*.25,size.y*.91), Vector2(size.x*.40,size.y*.84), Vector2(size.x*.55,size.y*.92),
        Vector2(size.x*.70,size.y*.83), Vector2(size.x*.86,size.y*.90), Vector2(size.x,size.y*.84),
        Vector2(size.x,size.y)
    ])
    draw_colored_polygon(near_hill, Color(0.015,0.05,0.035,0.86))
