extends Control

signal leaf_pressed(memory_id: String)
signal leaf_selected(memory_id: String)
signal memory_moved(memory_id: String, anchor_index: int)

var memories: Array = []
var arrange_mode := false
var selected_memory := ""

const ANCHORS := [
    Vector2(.50,.18), Vector2(.37,.24), Vector2(.63,.25), Vector2(.27,.33),
    Vector2(.48,.32), Vector2(.72,.34), Vector2(.18,.44), Vector2(.36,.43),
    Vector2(.58,.42), Vector2(.82,.45), Vector2(.27,.54), Vector2(.48,.53),
    Vector2(.70,.54), Vector2(.15,.59), Vector2(.85,.60), Vector2(.37,.64),
    Vector2(.61,.64), Vector2(.24,.71), Vector2(.76,.71), Vector2(.50,.73),
    Vector2(.33,.77), Vector2(.66,.77), Vector2(.42,.16), Vector2(.58,.16)
]

func _ready() -> void:
    custom_minimum_size = Vector2(640, 535)
    mouse_filter = Control.MOUSE_FILTER_STOP
    queue_redraw()

func _notification(what: int) -> void:
    if what == NOTIFICATION_RESIZED:
        queue_redraw()

func set_data(value: Array, arranging: bool) -> void:
    memories = value
    arrange_mode = arranging
    if not arranging:
        selected_memory = ""
    queue_redraw()

func _anchor(i: int) -> Vector2:
    var p: Vector2 = ANCHORS[i % ANCHORS.size()]
    return Vector2(p.x * size.x, p.y * size.y)

func _pos(m: Dictionary, i: int) -> Vector2:
    return _anchor(int(m.get("anchor", i % ANCHORS.size())))

func _color(kind: String) -> Color:
    match kind:
        "first": return Color("#f0c36c")
        "milestone": return Color("#79b891")
        "funny": return Color("#d99ab9")
        "health": return Color("#7caed2")
        "photo": return Color("#aa94d6")
        _: return Color("#8fbc78")

func _draw() -> void:
    if size.x < 20 or size.y < 20:
        return

    var w := size.x
    var h := size.y

    # Ground shadow.
    draw_ellipse(Vector2(w*.50,h*.90), Vector2(w*.22,h*.035), Color(0.0,0.0,0.0,.18))

    # Dense canopy painted once. No animation/redraw loop.
    var canopy := [
        [Vector2(.31,.36),.17],[Vector2(.46,.27),.18],[Vector2(.62,.29),.18],
        [Vector2(.72,.39),.16],[Vector2(.24,.48),.15],[Vector2(.42,.45),.19],
        [Vector2(.60,.46),.20],[Vector2(.78,.51),.13],[Vector2(.35,.58),.16],
        [Vector2(.57,.57),.18]
    ]
    for c in canopy:
        var center: Vector2 = c[0]
        var rad: float = float(c[1]) * w
        draw_circle(Vector2(center.x*w, center.y*h) + Vector2(0,7), rad, Color("#183d28"))
        draw_circle(Vector2(center.x*w, center.y*h), rad*.93, Color("#245337"))
        draw_circle(Vector2(center.x*w-rad*.18, center.y*h-rad*.18), rad*.58, Color(0.22,0.43,0.27,.56))

    # Tapered trunk.
    var trunk := PackedVector2Array([
        Vector2(w*.44,h*.90), Vector2(w*.47,h*.61), Vector2(w*.485,h*.39),
        Vector2(w*.515,h*.39), Vector2(w*.53,h*.61), Vector2(w*.56,h*.90)
    ])
    draw_colored_polygon(trunk, Color("#6a4329"))
    var trunk_light := PackedVector2Array([
        Vector2(w*.455,h*.88), Vector2(w*.485,h*.60), Vector2(w*.495,h*.40),
        Vector2(w*.507,h*.40), Vector2(w*.505,h*.62), Vector2(w*.49,h*.88)
    ])
    draw_colored_polygon(trunk_light, Color("#9a6841"))

    # Strong branches.
    var branches := [
        [Vector2(.50,.63),Vector2(.24,.46),15.0],[Vector2(.50,.59),Vector2(.76,.43),15.0],
        [Vector2(.50,.48),Vector2(.31,.29),12.0],[Vector2(.50,.45),Vector2(.69,.27),12.0],
        [Vector2(.49,.72),Vector2(.29,.64),13.0],[Vector2(.51,.70),Vector2(.71,.64),13.0]
    ]
    for pair in branches:
        var a: Vector2 = pair[0]
        var b: Vector2 = pair[1]
        var width: float = float(pair[2])
        var ap := Vector2(a.x*w,a.y*h)
        var bp := Vector2(b.x*w,b.y*h)
        draw_line(ap,bp,Color("#694128"),width,true)
        draw_line(ap+Vector2(-2,-2),bp+Vector2(-2,-2),Color("#95633d"),max(3.0,width*.28),true)

    # Fine branch connections to leaf anchors.
    for i in range(ANCHORS.size()):
        var p := _anchor(i)
        var source := Vector2(lerp(w*.50,p.x,.52), min(p.y+42,h*.72))
        draw_line(source,p,Color(0.34,0.22,0.14,.78),3.5,true)

    # Placement targets are only shown while arranging.
    if arrange_mode:
        for i in range(ANCHORS.size()):
            var p := _anchor(i)
            draw_circle(p,13.0,Color(0.95,0.78,0.40,.20))
            draw_arc(p,17.0,0,TAU,24,Color(0.96,0.82,0.47,.82),2.5)

    # Memory leaves.
    for i in range(memories.size()):
        var m: Dictionary = memories[i]
        var id := String(m.get("id",""))
        _draw_leaf(_pos(m,i), _color(String(m.get("kind","memory"))), id == selected_memory, i)

func draw_ellipse(center: Vector2, radii: Vector2, color: Color) -> void:
    var points := PackedVector2Array()
    for i in range(28):
        var a := TAU * float(i) / 28.0
        points.append(center + Vector2(cos(a)*radii.x, sin(a)*radii.y))
    draw_colored_polygon(points, color)

func _rot(v: Vector2, a: float) -> Vector2:
    var c := cos(a)
    var s := sin(a)
    return Vector2(v.x*c-v.y*s, v.x*s+v.y*c)

func _draw_leaf(p: Vector2, c: Color, selected: bool, i: int) -> void:
    var rx := 27.0 if not selected else 32.0
    var ry := 18.0 if not selected else 22.0
    var angle := deg_to_rad(float(((i * 37) % 31) - 15))
    var points := PackedVector2Array()
    for j in range(18):
        var a := TAU * float(j) / 18.0
        var local := Vector2(cos(a)*rx, sin(a)*ry)
        points.append(p + _rot(local, angle))
    draw_colored_polygon(points, c)

    # Small highlight gives each leaf depth.
    draw_arc(p + _rot(Vector2(-5,-4),angle), min(rx,ry)*.62, PI*1.05, PI*1.85, 10, Color(1,1,1,.16), 2.0)
    var vein := _rot(Vector2(rx*.72,0), angle)
    draw_line(p-vein,p+vein,Color(0.16,0.27,0.14,.62),1.8,true)

    if selected:
        draw_arc(p,rx+9,0,TAU,28,Color("#f3d58a"),4.0)

func _gui_input(event: InputEvent) -> void:
    var p := Vector2.ZERO
    var pressed := false
    if event is InputEventScreenTouch and event.pressed:
        p = event.position
        pressed = true
    elif event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
        p = event.position
        pressed = true
    if not pressed:
        return

    for i in range(memories.size()-1,-1,-1):
        var m: Dictionary = memories[i]
        if _pos(m,i).distance_to(p) <= 45.0:
            var id := String(m.get("id",""))
            if arrange_mode:
                selected_memory = id
                leaf_selected.emit(id)
                queue_redraw()
            else:
                leaf_pressed.emit(id)
            accept_event()
            return

    if arrange_mode and selected_memory != "":
        var nearest := -1
        var best := 999999.0
        for i in range(ANCHORS.size()):
            var d := _anchor(i).distance_to(p)
            if d < best:
                best = d
                nearest = i
        if nearest >= 0 and best < 78.0:
            memory_moved.emit(selected_memory,nearest)
            accept_event()
