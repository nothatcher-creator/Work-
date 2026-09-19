extends Control

signal leaf_pressed(memory_id: String)
signal leaf_selected(memory_id: String)
signal memory_moved(memory_id: String, anchor_index: int)

var memories: Array = []
var arrange_mode := false
var selected_memory := ""
var t := 0.0

const ANCHORS := [
    Vector2(.50,.20),Vector2(.37,.27),Vector2(.63,.28),Vector2(.28,.36),
    Vector2(.48,.35),Vector2(.71,.37),Vector2(.20,.48),Vector2(.38,.46),
    Vector2(.59,.46),Vector2(.80,.49),Vector2(.29,.58),Vector2(.49,.57),
    Vector2(.70,.58),Vector2(.16,.63),Vector2(.83,.63),Vector2(.39,.68),
    Vector2(.61,.68),Vector2(.26,.74),Vector2(.74,.74),Vector2(.50,.77)
]

func _ready() -> void:
    custom_minimum_size = Vector2(640, 600)
    mouse_filter = Control.MOUSE_FILTER_STOP
    set_process(true)

func set_data(value: Array, arranging: bool) -> void:
    memories = value
    arrange_mode = arranging
    if not arranging:
        selected_memory = ""
    queue_redraw()

func _process(delta: float) -> void:
    t += delta
    queue_redraw()

func _anchor(i: int) -> Vector2:
    var p: Vector2 = ANCHORS[i % ANCHORS.size()]
    return Vector2(p.x * size.x, p.y * size.y)

func _pos(m: Dictionary, i: int) -> Vector2:
    var p := _anchor(int(m.get("anchor", i % ANCHORS.size())))
    p.x += sin(t * 1.15 + i * .73) * 5.0
    p.y += sin(t * .82 + i * .41) * 2.5
    return p

func _color(kind: String) -> Color:
    match kind:
        "first": return Color("#f3c56d")
        "milestone": return Color("#8dcea0")
        "funny": return Color("#e5a2c2")
        "health": return Color("#8eb9df")
        "photo": return Color("#b4a1e4")
        _: return Color("#9bc382")

func _draw() -> void:
    if size.x < 20:
        return
    var w := size.x
    var h := size.y
    var base := Vector2(w*.50,h*.91)
    var top := Vector2(w*.50,h*.17)
    var alpha := 0.10 + min(memories.size(),16) * .007
    for c in [Vector2(.32,.38),Vector2(.50,.30),Vector2(.68,.39),Vector2(.40,.51),Vector2(.61,.52)]:
        draw_circle(Vector2(c.x*w,c.y*h), w*.16, Color(0.28,0.52,0.29,alpha))
    draw_line(base, top, Color("#5b3924"), 34.0, true)
    draw_line(base+Vector2(-5,0), top+Vector2(-4,8), Color("#8b5a35"), 11.0, true)
    var branches := [
        [Vector2(.50,.65),Vector2(.24,.47)],[Vector2(.50,.59),Vector2(.77,.44)],
        [Vector2(.50,.49),Vector2(.32,.31)],[Vector2(.50,.44),Vector2(.68,.29)],
        [Vector2(.50,.72),Vector2(.31,.66)],[Vector2(.50,.70),Vector2(.69,.66)]
    ]
    for pair in branches:
        var a: Vector2 = pair[0]
        var b: Vector2 = pair[1]
        draw_line(Vector2(a.x*w,a.y*h),Vector2(b.x*w,b.y*h),Color("#6d452a"),15.0,true)
        draw_line(Vector2(a.x*w-2,a.y*h),Vector2(b.x*w-2,b.y*h),Color("#98643c"),4.0,true)
    for i in range(ANCHORS.size()):
        var p := _anchor(i)
        var source := Vector2(lerp(w*.50,p.x,.48), min(p.y+48,h*.73))
        draw_line(source,p,Color(0.39,0.25,0.16,.8),4.5,true)
    if arrange_mode:
        for i in range(ANCHORS.size()):
            var p := _anchor(i)
            draw_circle(p,10.0,Color(0.98,0.83,0.46,.38))
            draw_arc(p,15.0,0,TAU,20,Color(0.98,0.83,0.46,.9),2.0)
    for i in range(memories.size()):
        var m: Dictionary = memories[i]
        _draw_leaf(_pos(m,i),_color(String(m.get("kind","memory"))),String(m.get("id",""))==selected_memory,i)

func _draw_leaf(p: Vector2, c: Color, selected: bool, i: int) -> void:
    var r := 25.0 if not selected else 30.0
    var a := sin(t*.9+i)*.17
    var d := Vector2(cos(a),sin(a))
    var s := Vector2(-d.y,d.x)
    var pts := PackedVector2Array([p+d*r,p+s*r*.72,p-d*r,p-s*r*.72])
    draw_colored_polygon(pts,c)
    draw_line(p-d*r*.66,p+d*r*.66,Color(0.18,0.29,0.16,.7),2.0,true)
    if selected:
        draw_arc(p,r+9,0,TAU,24,Color("#f5d88c"),4.0)

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
        if _pos(m,i).distance_to(p) <= 43.0:
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
        if nearest >= 0 and best < 70.0:
            memory_moved.emit(selected_memory,nearest)
            accept_event()
