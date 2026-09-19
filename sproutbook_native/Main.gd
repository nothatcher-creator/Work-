extends Control

const MemoryTree = preload("res://MemoryTree.gd")
const ForestBackground = preload("res://ForestBackground.gd")
const LongPressButton = preload("res://LongPressButton.gd")

const SAVE_PATH := "user://sproutbook_native_v200.json"
const ACCENT := Color("#d7b98e")
const BG_PANEL := Color(0.055,0.105,0.082,0.95)
const BG_PANEL_2 := Color(0.075,0.135,0.105,0.97)
const TEXT := Color("#f1f4ee")
const MUTED := Color("#afbeb4")
const BORDER := Color(0.44,0.60,0.50,0.45)

var state: Dictionary = {}
var current_page := "home"
var current_tool := ""
var arranging_tree := false
var calendar_year := 0
var calendar_month := 0
var selected_calendar_date := ""

var content: VBoxContainer
var scroller: ScrollContainer
var child_select: OptionButton
var mode_button: Button
var nav_buttons: Dictionary = {}
var modal_layer: Control

func _ready() -> void:
    _make_default_state()
    _load_state()
    _ensure_state()
    _set_calendar_today()
    _build_shell()
    show_page("home")

func _make_default_state() -> void:
    state = {
        "children": [{
            "id":"child_1",
            "name":"My Child",
            "mode":"Baby",
            "birthday":"",
            "memories":[],
            "feeds":[],
            "solids":[],
            "teeth":{},
            "milestones":{},
            "sleep":[]
        }],
        "selected":"child_1",
        "appointments":[],
        "inventory":[],
        "emergency":{"allergies":"","meds":"","conditions":"","doctor":"","doctor_phone":"","contacts":""},
        "settings":{"grandparent":false,"reduce_motion":false}
    }

func _load_state() -> void:
    if not FileAccess.file_exists(SAVE_PATH):
        return
    var file := FileAccess.open(SAVE_PATH, FileAccess.READ)
    if file == null:
        return
    var parsed = JSON.parse_string(file.get_as_text())
    if typeof(parsed) == TYPE_DICTIONARY:
        state = parsed

func _save_state() -> void:
    var file := FileAccess.open(SAVE_PATH, FileAccess.WRITE)
    if file != null:
        file.store_string(JSON.stringify(state, "  "))

func _ensure_state() -> void:
    if not state.has("children") or not (state.children is Array) or state.children.is_empty():
        _make_default_state()
    state["appointments"] = state.get("appointments", [])
    state["inventory"] = state.get("inventory", [])
    state["settings"] = state.get("settings", {"grandparent":false,"reduce_motion":false})
    state["emergency"] = state.get("emergency", {})
    if not state.has("selected"):
        state.selected = state.children[0].id
    for c in state.children:
        c["name"] = c.get("name","Child")
        c["mode"] = c.get("mode","Baby")
        c["birthday"] = c.get("birthday","")
        c["memories"] = c.get("memories",[])
        c["feeds"] = c.get("feeds",[])
        c["solids"] = c.get("solids",[])
        c["teeth"] = c.get("teeth",{})
        c["milestones"] = c.get("milestones",{})
        c["sleep"] = c.get("sleep",[])

func _build_shell() -> void:
    var bg = ForestBackground.new()
    bg.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    add_child(bg)

    var root := VBoxContainer.new()
    root.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    root.add_theme_constant_override("separation", 0)
    add_child(root)

    var top_panel := PanelContainer.new()
    top_panel.custom_minimum_size = Vector2(0, 108)
    top_panel.add_theme_stylebox_override("panel", _style(BG_PANEL_2, 0, Color.TRANSPARENT, 0))
    root.add_child(top_panel)

    var top_margin := MarginContainer.new()
    _margins(top_margin, 22, 18, 22, 14)
    top_panel.add_child(top_margin)

    var top_row := HBoxContainer.new()
    top_row.add_theme_constant_override("separation", 12)
    top_margin.add_child(top_row)

    child_select = OptionButton.new()
    child_select.custom_minimum_size = Vector2(210, 64)
    child_select.add_theme_font_size_override("font_size", 23)
    child_select.item_selected.connect(_on_child_selected)
    top_row.add_child(child_select)

    mode_button = Button.new()
    mode_button.custom_minimum_size = Vector2(180, 64)
    mode_button.add_theme_font_size_override("font_size", 21)
    mode_button.pressed.connect(Callable(self,"show_tool").bind("profile"))
    top_row.add_child(mode_button)

    var spacer := Control.new()
    spacer.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    top_row.add_child(spacer)

    var brand := _label("SproutBook", 23, ACCENT)
    top_row.add_child(brand)

    scroller = ScrollContainer.new()
    scroller.size_flags_vertical = Control.SIZE_EXPAND_FILL
    scroller.horizontal_scroll_mode = ScrollContainer.SCROLL_MODE_DISABLED
    root.add_child(scroller)

    var page_margin := MarginContainer.new()
    page_margin.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    _margins(page_margin, 22, 22, 22, 28)
    scroller.add_child(page_margin)

    content = VBoxContainer.new()
    content.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    content.add_theme_constant_override("separation", 18)
    page_margin.add_child(content)

    var nav_panel := PanelContainer.new()
    nav_panel.custom_minimum_size = Vector2(0, 110)
    nav_panel.add_theme_stylebox_override("panel", _style(Color(0.045,0.075,0.058,0.99), 0, Color.TRANSPARENT, 0))
    root.add_child(nav_panel)

    var nav_margin := MarginContainer.new()
    _margins(nav_margin, 14, 10, 14, 10)
    nav_panel.add_child(nav_margin)

    var nav := HBoxContainer.new()
    nav.add_theme_constant_override("separation", 10)
    nav_margin.add_child(nav)

    for item in [["home","Home"],["schedule","Schedule"],["parenting","Parenting"],["more","More"]]:
        var key: String = item[0]
        var button := Button.new()
        button.text = item[1]
        button.size_flags_horizontal = Control.SIZE_EXPAND_FILL
        button.custom_minimum_size = Vector2(0, 80)
        button.add_theme_font_size_override("font_size", 19)
        button.pressed.connect(Callable(self,"show_page").bind(key))
        nav.add_child(button)
        nav_buttons[key] = button

    modal_layer = Control.new()
    modal_layer.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    modal_layer.mouse_filter = Control.MOUSE_FILTER_IGNORE
    modal_layer.z_index = 50
    add_child(modal_layer)

    _refresh_child_selector()

func _style(color: Color, radius := 24, border_color := BORDER, border_width := 1) -> StyleBoxFlat:
    var s := StyleBoxFlat.new()
    s.bg_color = color
    s.corner_radius_top_left = radius
    s.corner_radius_top_right = radius
    s.corner_radius_bottom_left = radius
    s.corner_radius_bottom_right = radius
    s.border_width_left = border_width
    s.border_width_top = border_width
    s.border_width_right = border_width
    s.border_width_bottom = border_width
    s.border_color = border_color
    s.content_margin_left = 18
    s.content_margin_right = 18
    s.content_margin_top = 16
    s.content_margin_bottom = 16
    return s

func _margins(m: MarginContainer, left: int, top: int, right: int, bottom: int) -> void:
    m.add_theme_constant_override("margin_left", left)
    m.add_theme_constant_override("margin_top", top)
    m.add_theme_constant_override("margin_right", right)
    m.add_theme_constant_override("margin_bottom", bottom)

func _label(value: String, font_size := 21, color := TEXT) -> Label:
    var l := Label.new()
    l.text = value
    l.add_theme_font_size_override("font_size", font_size)
    l.add_theme_color_override("font_color", color)
    l.autowrap_mode = TextServer.AUTOWRAP_WORD_SMART
    return l

func _button(value: String, action: Callable, primary := false) -> Button:
    var b := Button.new()
    b.text = value
    b.custom_minimum_size = Vector2(0, 60)
    b.add_theme_font_size_override("font_size", 20)
    b.add_theme_stylebox_override("normal", _style(Color(0.29,0.27,0.20,0.96) if primary else BG_PANEL_2, 18, ACCENT if primary else BORDER, 2 if primary else 1))
    b.add_theme_stylebox_override("pressed", _style(Color(0.23,0.30,0.22,1), 18, ACCENT, 2))
    b.pressed.connect(action)
    return b

func _card(title: String, subtitle := "") -> VBoxContainer:
    var panel := PanelContainer.new()
    panel.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    panel.add_theme_stylebox_override("panel", _style(BG_PANEL, 28, BORDER, 1))
    content.add_child(panel)
    var box := VBoxContainer.new()
    box.add_theme_constant_override("separation", 12)
    panel.add_child(box)
    if title != "":
        box.add_child(_label(title, 27, TEXT))
    if subtitle != "":
        box.add_child(_label(subtitle, 18, MUTED))
    return box

func _section_title(value: String) -> void:
    content.add_child(_label(value, 32, TEXT))

func _clear_content() -> void:
    for node in content.get_children():
        node.queue_free()
    scroller.scroll_vertical = 0

func _selected_child() -> Dictionary:
    for c in state.children:
        if String(c.id) == String(state.selected):
            return c
    state.selected = state.children[0].id
    return state.children[0]

func _refresh_child_selector() -> void:
    child_select.clear()
    var selected_index := 0
    for i in range(state.children.size()):
        var c: Dictionary = state.children[i]
        child_select.add_item(String(c.name))
        child_select.set_item_metadata(i, c.id)
        if String(c.id) == String(state.selected):
            selected_index = i
    child_select.select(selected_index)
    var c := _selected_child()
    mode_button.text = String(c.mode) + " mode"

func _on_child_selected(index: int) -> void:
    state.selected = child_select.get_item_metadata(index)
    _save_state()
    _refresh_child_selector()
    refresh_current()

func show_page(page: String) -> void:
    current_page = page
    current_tool = ""
    arranging_tree = false
    _clear_content()
    _refresh_child_selector()
    match page:
        "home":
            _build_home()
        "schedule":
            _build_schedule()
        "parenting":
            _build_parenting()
        "more":
            _build_more()
        _:
            _build_home()
    _refresh_nav()

func show_tool(tool: String) -> void:
    current_tool = tool
    _clear_content()
    content.add_child(_button("‹ Back", Callable(self,"show_page").bind(current_page)))
    match tool:
        "feeding":
            _build_feeding()
        "solids":
            _build_solids()
        "teeth":
            _build_teeth()
        "milestones":
            _build_milestones()
        "sleep":
            _build_sleep()
        "calm":
            _build_calm()
        "inventory":
            _build_inventory()
        "emergency":
            _build_emergency()
        "profile":
            _build_profile()
        "backup":
            _build_backup()
        _:
            _build_parenting()

func refresh_current() -> void:
    if current_tool != "":
        show_tool(current_tool)
    else:
        show_page(current_page)

func _refresh_nav() -> void:
    for key in nav_buttons.keys():
        var b: Button = nav_buttons[key]
        var active := key == current_page
        b.add_theme_stylebox_override("normal", _style(Color(0.18,0.17,0.12,0.97) if active else Color(0.05,0.08,0.06,0.90), 22, ACCENT if active else Color.TRANSPARENT, 2 if active else 0))
        b.add_theme_color_override("font_color", TEXT if active else MUTED)

func _build_home() -> void:
    var c := _selected_child()
    _section_title("Today with " + String(c.name))

    var quick := _card("Quick actions", "The common stuff is always one tap away.")
    var qgrid := GridContainer.new()
    qgrid.columns = 2
    qgrid.add_theme_constant_override("h_separation", 10)
    qgrid.add_theme_constant_override("v_separation", 10)
    quick.add_child(qgrid)
    qgrid.add_child(_button("Log feeding", Callable(self,"show_tool").bind("feeding"), true))
    qgrid.add_child(_button("Sleep log", Callable(self,"show_tool").bind("sleep")))
    qgrid.add_child(_button("Add memory", Callable(self,"_open_add_memory")))
    qgrid.add_child(_button("Help right now", Callable(self,"show_tool").bind("calm")))

    var tree_box := _card("Memory Tree", "Each memory grows a leaf. Arrange leaves yourself, or let SproutBook place them automatically.")
    var actions := HBoxContainer.new()
    actions.add_theme_constant_override("separation", 10)
    tree_box.add_child(actions)
    actions.add_child(_button("+ Memory", Callable(self,"_open_add_memory"), true))
    actions.add_child(_button("Done arranging" if arranging_tree else "Arrange leaves", Callable(self,"_toggle_arrange")))
    var counter := _label(str(c.memories.size()) + " leaves", 18, MUTED)
    counter.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    counter.horizontal_alignment = HORIZONTAL_ALIGNMENT_RIGHT
    actions.add_child(counter)

    var tree = MemoryTree.new()
    tree.name = "MemoryTree"
    tree.set_data(c.memories, arranging_tree)
    tree.leaf_pressed.connect(_open_memory_detail)
    tree.leaf_selected.connect(_tree_leaf_selected)
    tree.memory_moved.connect(_move_memory)
    tree_box.add_child(tree)

    if c.memories.is_empty():
        var empty := _card("Plant the first leaf", "First laugh, first food, a funny quote, a milestone, a rough day you survived — anything worth remembering.")
        empty.add_child(_button("Add first memory", Callable(self,"_open_add_memory"), true))
    else:
        var recent := _card("Recent memories", "Tap a leaf on the tree for details, or open one here.")
        var start := max(0, c.memories.size() - 4)
        for i in range(c.memories.size() - 1, start - 1, -1):
            var m: Dictionary = c.memories[i]
            var mid := String(m.id)
            recent.add_child(_button(String(m.title) + " · " + String(m.date), Callable(self,"_open_memory_detail").bind(mid)))

func _toggle_arrange() -> void:
    arranging_tree = not arranging_tree
    refresh_current()

func _tree_leaf_selected(_id: String) -> void:
    _toast("Leaf selected — tap a glowing branch spot to move it.")

func _open_add_memory() -> void:
    var box := VBoxContainer.new()
    box.add_theme_constant_override("separation", 10)
    var title := LineEdit.new()
    title.placeholder_text = "Memory title"
    title.custom_minimum_size = Vector2(0, 58)
    box.add_child(title)
    var kind := OptionButton.new()
    for item in ["Memory","First","Milestone","Funny","Health","Photo"]:
        kind.add_item(item)
    kind.custom_minimum_size = Vector2(0, 58)
    box.add_child(kind)
    var notes := TextEdit.new()
    notes.placeholder_text = "What happened?"
    notes.custom_minimum_size = Vector2(0, 150)
    box.add_child(notes)
    var save := _button("Add leaf to tree", Callable(self,"_save_new_memory").bind(title,kind,notes), true)
    box.add_child(save)
    _show_modal("New memory", box)

func _save_new_memory(title: LineEdit, kind: OptionButton, notes: TextEdit) -> void:
    if title.text.strip_edges() == "":
        _toast("Give the memory a title first.")
        return
    if _readonly_guard():
        return
    var c := _selected_child()
    c.memories.append({
        "id":_uid("mem"),
        "title":title.text.strip_edges(),
        "kind":kind.get_item_text(kind.selected).to_lower(),
        "notes":notes.text.strip_edges(),
        "date":_today_string(),
        "anchor":c.memories.size() % 20
    })
    _save_state()
    _hide_modal()
    refresh_current()
    _toast("A new leaf grew on the tree.")

func _open_memory_detail(memory_id: String) -> void:
    var c := _selected_child()
    var found: Dictionary = {}
    for m in c.memories:
        if String(m.id) == memory_id:
            found = m
            break
    if found.is_empty():
        return
    var box := VBoxContainer.new()
    box.add_theme_constant_override("separation", 10)
    box.add_child(_label(String(found.title), 28, ACCENT))
    box.add_child(_label(String(found.date) + " · " + String(found.kind).capitalize(), 18, MUTED))
    box.add_child(_label(String(found.get("notes","No notes.")), 20, TEXT))
    box.add_child(_button("Arrange this leaf", Callable(self,"_arrange_memory").bind(memory_id)))
    box.add_child(_button("Delete memory", Callable(self,"_delete_memory").bind(memory_id)))
    _show_modal("Leaf memory", box)

func _arrange_memory(memory_id: String) -> void:
    _hide_modal()
    arranging_tree = true
    refresh_current()
    await get_tree().process_frame
    var tree := find_child("MemoryTree", true, false)
    if tree != null:
        tree.selected_memory = memory_id
        tree.queue_redraw()
    _toast("Tap a glowing branch spot to move the leaf.")

func _delete_memory(memory_id: String) -> void:
    if _readonly_guard():
        return
    var c := _selected_child()
    for i in range(c.memories.size() - 1, -1, -1):
        if String(c.memories[i].id) == memory_id:
            c.memories.remove_at(i)
            break
    _save_state()
    _hide_modal()
    refresh_current()

func _move_memory(memory_id: String, anchor_index: int) -> void:
    if _readonly_guard():
        return
    var c := _selected_child()
    for m in c.memories:
        if String(m.id) == memory_id:
            m.anchor = anchor_index
            break
    _save_state()
    refresh_current()
    _toast("Leaf moved.")

func _build_schedule() -> void:
    _section_title("Schedule")
    var cal := _card("Calendar", "Tap a day to inspect it. Long-press any day to start an appointment on that date.")
    var header := HBoxContainer.new()
    header.add_child(_button("‹", Callable(self,"_calendar_prev")))
    var month_label := _label(_month_name(calendar_month) + " " + str(calendar_year), 26, TEXT)
    month_label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    month_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    header.add_child(month_label)
    header.add_child(_button("›", Callable(self,"_calendar_next")))
    cal.add_child(header)

    var grid := GridContainer.new()
    grid.columns = 7
    grid.add_theme_constant_override("h_separation", 5)
    grid.add_theme_constant_override("v_separation", 5)
    cal.add_child(grid)

    for day_name in ["S","M","T","W","T","F","S"]:
        var h := _label(day_name, 16, MUTED)
        h.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
        grid.add_child(h)

    var first := _weekday(calendar_year, calendar_month, 1)
    for i in range(first):
        var blank := Control.new()
        blank.custom_minimum_size = Vector2(80, 62)
        grid.add_child(blank)

    for day in range(1, _days_in_month(calendar_year,calendar_month) + 1):
        var date := _date_string(calendar_year,calendar_month,day)
        var b = LongPressButton.new()
        b.text = str(day) + ("\n•" if _day_mark_count(date) > 0 else "")
        b.custom_minimum_size = Vector2(80, 70)
        b.add_theme_font_size_override("font_size", 18)
        b.add_theme_stylebox_override("normal", _style(Color(0.07,0.12,0.09,0.95), 16, ACCENT if date == selected_calendar_date else BORDER, 2 if date == selected_calendar_date else 1))
        b.pressed.connect(Callable(self,"_select_calendar_date").bind(date))
        b.long_pressed.connect(Callable(self,"_open_appointment").bind(date))
        grid.add_child(b)

    var detail := _card("Selected day · " + selected_calendar_date, "Birthdays and appointments.")
    var anything := false
    for c in state.children:
        if _birthday_matches(c, selected_calendar_date):
            detail.add_child(_label("Birthday · " + String(c.name), 20, ACCENT))
            anything = true
    for a in state.appointments:
        if String(a.get("date","")) == selected_calendar_date:
            detail.add_child(_label(String(a.get("time","")) + "  " + String(a.get("title","Appointment")), 20, TEXT))
            anything = true
    if not anything:
        detail.add_child(_label("Nothing scheduled.", 18, MUTED))
    detail.add_child(_button("+ Appointment on this day", Callable(self,"_open_appointment").bind(selected_calendar_date), true))

    var upcoming := _card("Upcoming appointments", "Everything in one list.")
    upcoming.add_child(_button("+ New appointment", Callable(self,"_open_appointment").bind(_today_string()), true))
    if state.appointments.is_empty():
        upcoming.add_child(_label("No appointments yet.", 18, MUTED))
    else:
        for a in state.appointments:
            var row := HBoxContainer.new()
            var text := _label(String(a.get("date","")) + " " + String(a.get("time","")) + "\n" + String(a.get("title","Appointment")), 18, TEXT)
            text.size_flags_horizontal = Control.SIZE_EXPAND_FILL
            row.add_child(text)
            row.add_child(_button("Delete", Callable(self,"_delete_appointment").bind(String(a.id))))
            upcoming.add_child(row)

func _select_calendar_date(date: String) -> void:
    selected_calendar_date = date
    refresh_current()

func _open_appointment(date_value: String) -> void:
    var box := VBoxContainer.new()
    box.add_theme_constant_override("separation", 10)
    var title := LineEdit.new()
    title.placeholder_text = "Appointment title"
    title.custom_minimum_size = Vector2(0,58)
    box.add_child(title)
    var date := LineEdit.new()
    date.text = date_value
    date.placeholder_text = "YYYY-MM-DD"
    date.custom_minimum_size = Vector2(0,58)
    box.add_child(date)
    var time := LineEdit.new()
    time.placeholder_text = "Time, e.g. 09:30"
    time.custom_minimum_size = Vector2(0,58)
    box.add_child(time)
    var location := LineEdit.new()
    location.placeholder_text = "Location"
    location.custom_minimum_size = Vector2(0,58)
    box.add_child(location)
    box.add_child(_button("Save appointment", Callable(self,"_save_appointment").bind(title,date,time,location), true))
    _show_modal("New appointment", box)

func _save_appointment(title: LineEdit, date: LineEdit, time: LineEdit, location: LineEdit) -> void:
    if title.text.strip_edges() == "":
        _toast("Add a title first.")
        return
    if _readonly_guard():
        return
    state.appointments.append({
        "id":_uid("appt"),
        "title":title.text.strip_edges(),
        "date":date.text.strip_edges(),
        "time":time.text.strip_edges(),
        "location":location.text.strip_edges()
    })
    selected_calendar_date = date.text.strip_edges()
    _save_state()
    _hide_modal()
    refresh_current()
    _toast("Appointment saved.")

func _delete_appointment(id: String) -> void:
    if _readonly_guard():
        return
    for i in range(state.appointments.size()-1,-1,-1):
        if String(state.appointments[i].id) == id:
            state.appointments.remove_at(i)
            break
    _save_state()
    refresh_current()

func _build_parenting() -> void:
    _section_title("Parenting")
    var intro := _card("One-stop parent tools", "Big cards, fewer nested menus, and the current child's stage stays visible at the top.")
    intro.add_child(_label("Current stage: " + String(_selected_child().mode), 20, ACCENT))
    var grid := GridContainer.new()
    grid.columns = 2
    grid.add_theme_constant_override("h_separation", 10)
    grid.add_theme_constant_override("v_separation", 10)
    content.add_child(grid)
    for item in [
        ["feeding","Feeding","Bottle, breast & pump"],
        ["solids","Solids","Foods tried & reactions"],
        ["sleep","Sleep","Start/stop sleep sessions"],
        ["teeth","Teeth","Tap to cycle tooth stage"],
        ["milestones","Milestones","Stage checklist"],
        ["calm","Help right now","Three calm steps first"]
    ]:
        var p := PanelContainer.new()
        p.custom_minimum_size = Vector2(310, 140)
        p.add_theme_stylebox_override("panel", _style(BG_PANEL, 24, BORDER, 1))
        var v := VBoxContainer.new()
        v.add_theme_constant_override("separation", 6)
        p.add_child(v)
        v.add_child(_label(item[1], 23, TEXT))
        v.add_child(_label(item[2], 16, MUTED))
        v.add_child(_button("Open", Callable(self,"show_tool").bind(item[0]), item[0] == "calm"))
        grid.add_child(p)

func _build_more() -> void:
    _section_title("More")
    var grid := GridContainer.new()
    grid.columns = 2
    grid.add_theme_constant_override("h_separation", 10)
    grid.add_theme_constant_override("v_separation", 10)
    content.add_child(grid)
    for item in [["inventory","Inventory"],["emergency","Emergency card"],["profile","Profile + settings"],["backup","Backup / import"]]:
        var b := _button(item[1], Callable(self,"show_tool").bind(item[0]), item[0] == "emergency")
        b.custom_minimum_size = Vector2(310, 90)
        grid.add_child(b)
    var gp := _card("Grandparent mode", "A read-only mode for caregivers who only need to see information.")
    var toggle := CheckButton.new()
    toggle.text = "Grandparent read-only"
    toggle.button_pressed = bool(state.settings.get("grandparent",false))
    toggle.add_theme_font_size_override("font_size", 20)
    toggle.toggled.connect(_toggle_grandparent)
    gp.add_child(toggle)

func _toggle_grandparent(value: bool) -> void:
    state.settings.grandparent = value
    _save_state()
    _toast("Grandparent mode " + ("on" if value else "off"))

func _build_feeding() -> void:
    _section_title("Feeding")
    var c := _selected_child()
    var form := _card("Quick feeding log", "Save bottle, breast, or pumping without digging through separate screens.")
    var kind := OptionButton.new()
    for item in ["Bottle","Breast","Pump"]:
        kind.add_item(item)
    kind.custom_minimum_size = Vector2(0,58)
    form.add_child(kind)
    var amount := LineEdit.new()
    amount.placeholder_text = "Amount or minutes"
    amount.custom_minimum_size = Vector2(0,58)
    form.add_child(amount)
    var note := LineEdit.new()
    note.placeholder_text = "Note (optional)"
    note.custom_minimum_size = Vector2(0,58)
    form.add_child(note)
    form.add_child(_button("Save feeding", Callable(self,"_save_feeding").bind(kind,amount,note), true))

    var recent := _card("Recent feedings", "")
    if c.feeds.is_empty():
        recent.add_child(_label("No feedings logged yet.",18,MUTED))
    else:
        var start := max(0,c.feeds.size()-8)
        for i in range(c.feeds.size()-1,start-1,-1):
            var f: Dictionary = c.feeds[i]
            recent.add_child(_label(String(f.kind) + " · " + String(f.amount) + " · " + String(f.time),18,TEXT))

func _save_feeding(kind: OptionButton, amount: LineEdit, note: LineEdit) -> void:
    if _readonly_guard():
        return
    var c := _selected_child()
    c.feeds.append({"id":_uid("feed"),"kind":kind.get_item_text(kind.selected),"amount":amount.text,"note":note.text,"time":_now_string()})
    _save_state()
    refresh_current()
    _toast("Feeding saved.")

func _build_solids() -> void:
    _section_title("Solids")
    var guide := _card("Starting solids", "Many babies are ready around 6 months when they can control their head/neck, sit with support, reach for food and swallow it rather than push it back out.")
    guide.add_child(_label("Use soft, age-appropriate textures and avoid choking hazards such as whole nuts, popcorn, hard raw chunks and whole grapes. Follow your baby's clinician if they recommend a different plan.",18,MUTED))
    var c := _selected_child()
    var form := _card("Food tracker", "Record what was tried and how it went.")
    var food := LineEdit.new()
    food.placeholder_text = "Food"
    food.custom_minimum_size = Vector2(0,58)
    form.add_child(food)
    var reaction := OptionButton.new()
    for item in ["Loved it","Okay","Refused","Possible reaction"]:
        reaction.add_item(item)
    reaction.custom_minimum_size = Vector2(0,58)
    form.add_child(reaction)
    form.add_child(_button("Add food", Callable(self,"_save_solid").bind(food,reaction), true))
    var list := _card("Tried foods", "")
    if c.solids.is_empty():
        list.add_child(_label("Nothing logged yet.",18,MUTED))
    else:
        for i in range(c.solids.size()-1,-1,-1):
            var s: Dictionary = c.solids[i]
            list.add_child(_label(String(s.food) + " · " + String(s.reaction) + " · " + String(s.date),18,TEXT))

func _save_solid(food: LineEdit, reaction: OptionButton) -> void:
    if _readonly_guard() or food.text.strip_edges() == "":
        return
    var c := _selected_child()
    c.solids.append({"id":_uid("food"),"food":food.text.strip_edges(),"reaction":reaction.get_item_text(reaction.selected),"date":_today_string()})
    _save_state()
    refresh_current()

func _build_teeth() -> void:
    _section_title("Teeth")
    var c := _selected_child()
    var card := _card("Tap a tooth to cycle", "Not seen → observed → erupted. Native controls mean no browser text-selection bug.")
    var grid := GridContainer.new()
    grid.columns = 5
    grid.add_theme_constant_override("h_separation", 8)
    grid.add_theme_constant_override("v_separation", 8)
    card.add_child(grid)
    for i in range(20):
        var key := str(i)
        var status := String(c.teeth.get(key,"none"))
        var b := Button.new()
        b.text = str(i+1) + "\n" + ("—" if status == "none" else ("Seen" if status == "seen" else "Up"))
        b.custom_minimum_size = Vector2(110,78)
        b.add_theme_font_size_override("font_size",16)
        var color := Color(0.07,0.12,0.09,1)
        if status == "seen":
            color = Color(0.18,0.25,0.22,1)
        elif status == "erupted":
            color = Color(0.33,0.31,0.20,1)
        b.add_theme_stylebox_override("normal", _style(color,16,ACCENT if status == "erupted" else BORDER,2 if status == "erupted" else 1))
        b.pressed.connect(Callable(self,"_cycle_tooth").bind(key))
        grid.add_child(b)

func _cycle_tooth(key: String) -> void:
    if _readonly_guard():
        return
    var c := _selected_child()
    var cur := String(c.teeth.get(key,"none"))
    c.teeth[key] = "seen" if cur == "none" else ("erupted" if cur == "seen" else "none")
    _save_state()
    refresh_current()

func _build_milestones() -> void:
    _section_title("Milestones")
    var c := _selected_child()
    var card := _card("Stage checklist", "Use this as a memory and organization tool, not a pass/fail test. Kids develop at different rates.")
    for item in _milestones_for_mode(String(c.mode)):
        var cb := CheckBox.new()
        cb.text = item
        cb.button_pressed = bool(c.milestones.get(item,false))
        cb.add_theme_font_size_override("font_size",20)
        cb.toggled.connect(Callable(self,"_set_milestone").bind(item,cb))
        card.add_child(cb)

func _set_milestone(value: bool, item: String, checkbox: CheckBox) -> void:
    if _readonly_guard():
        checkbox.button_pressed = not value
        return
    var c := _selected_child()
    c.milestones[item] = value
    _save_state()

func _build_sleep() -> void:
    _section_title("Sleep")
    var c := _selected_child()
    var card := _card("Sleep log", "Save a quick sleep entry. A full native sound machine can be layered into this screen without a WebView.")
    var mins := SpinBox.new()
    mins.min_value = 1
    mins.max_value = 1440
    mins.value = 60
    mins.suffix = " min"
    mins.custom_minimum_size = Vector2(0,58)
    card.add_child(mins)
    card.add_child(_button("Save sleep", Callable(self,"_save_sleep").bind(mins), true))
    var recent := _card("Recent sleep", "")
    if c.sleep.is_empty():
        recent.add_child(_label("No sleep logged yet.",18,MUTED))
    else:
        var start := max(0,c.sleep.size()-8)
        for i in range(c.sleep.size()-1,start-1,-1):
            recent.add_child(_label(str(c.sleep[i].minutes) + " min · " + String(c.sleep[i].time),18,TEXT))

func _save_sleep(mins: SpinBox) -> void:
    if _readonly_guard():
        return
    _selected_child().sleep.append({"minutes":int(mins.value),"time":_now_string()})
    _save_state()
    refresh_current()

func _build_calm() -> void:
    _section_title("What do I do right now?")
    var card := _card("Pick the problem", "Three calm steps first. Longer context appears underneath.")
    var choose := OptionButton.new()
    for item in ["Baby won't stop crying","Baby won't latch","Gas or spit-up","Fever / seems unwell","I'm overwhelmed","Choking or trouble breathing"]:
        choose.add_item(item)
    choose.custom_minimum_size = Vector2(0,60)
    card.add_child(choose)
    var out := VBoxContainer.new()
    out.add_theme_constant_override("separation",8)
    card.add_child(out)
    card.add_child(_button("Show 3 steps", Callable(self,"_show_calm_steps").bind(choose,out), true))

func _show_calm_steps(choose: OptionButton, out: VBoxContainer) -> void:
    for node in out.get_children():
        node.queue_free()
    var steps := _calm_steps(choose.get_item_text(choose.selected))
    out.add_child(_label("1. " + steps[0],20,TEXT))
    out.add_child(_label("2. " + steps[1],20,TEXT))
    out.add_child(_label("3. " + steps[2],20,TEXT))
    out.add_child(_label(steps[3],17,MUTED))

func _build_inventory() -> void:
    _section_title("Inventory")
    var form := _card("Add supply", "Diapers, wipes, formula, toiletries, medicine — whatever you want to track.")
    var name := LineEdit.new()
    name.placeholder_text = "Item"
    name.custom_minimum_size = Vector2(0,58)
    form.add_child(name)
    var qty := SpinBox.new()
    qty.min_value = 0
    qty.max_value = 999
    qty.value = 1
    qty.custom_minimum_size = Vector2(0,58)
    form.add_child(qty)
    form.add_child(_button("Add item", Callable(self,"_add_inventory").bind(name,qty), true))
    var list := _card("Supplies", "")
    if state.inventory.is_empty():
        list.add_child(_label("No items yet.",18,MUTED))
    else:
        for item in state.inventory:
            var row := HBoxContainer.new()
            var l := _label(String(item.name) + " · " + str(item.qty),19,TEXT)
            l.size_flags_horizontal = Control.SIZE_EXPAND_FILL
            row.add_child(l)
            row.add_child(_button("−", Callable(self,"_inventory_change").bind(String(item.id),-1)))
            row.add_child(_button("+", Callable(self,"_inventory_change").bind(String(item.id),1)))
            list.add_child(row)

func _add_inventory(name: LineEdit, qty: SpinBox) -> void:
    if _readonly_guard() or name.text.strip_edges() == "":
        return
    state.inventory.append({"id":_uid("inv"),"name":name.text.strip_edges(),"qty":int(qty.value)})
    _save_state()
    refresh_current()

func _inventory_change(id: String, delta: int) -> void:
    if _readonly_guard():
        return
    for item in state.inventory:
        if String(item.id) == id:
            item.qty = max(0,int(item.qty)+delta)
    _save_state()
    refresh_current()

func _build_emergency() -> void:
    _section_title("Emergency card")
    var e: Dictionary = state.emergency
    var card := _card("Important information", "Stored on this device unless you export a backup.")
    var fields := {}
    for spec in [["allergies","Allergies"],["meds","Medications"],["conditions","Conditions / notes"],["doctor","Doctor / clinic"],["doctor_phone","Doctor phone"],["contacts","Emergency contacts"]]:
        var edit := LineEdit.new()
        edit.placeholder_text = spec[1]
        edit.text = String(e.get(spec[0],""))
        edit.custom_minimum_size = Vector2(0,58)
        card.add_child(edit)
        fields[spec[0]] = edit
    card.add_child(_button("Save emergency card", Callable(self,"_save_emergency").bind(fields), true))
    card.add_child(_button("Copy card", Callable(self,"_copy_emergency")))

func _save_emergency(fields: Dictionary) -> void:
    if _readonly_guard():
        return
    for key in fields.keys():
        state.emergency[key] = fields[key].text.strip_edges()
    _save_state()
    _toast("Emergency card saved.")

func _copy_emergency() -> void:
    var e: Dictionary = state.emergency
    var text := "Allergies: %s\nMedications: %s\nConditions: %s\nDoctor: %s %s\nEmergency contacts: %s" % [e.get("allergies",""),e.get("meds",""),e.get("conditions",""),e.get("doctor",""),e.get("doctor_phone",""),e.get("contacts","")]
    DisplayServer.clipboard_set(text)
    _toast("Emergency card copied.")

func _build_profile() -> void:
    _section_title("Profile + settings")
    var c := _selected_child()
    var card := _card("Current child", "")
    var name := LineEdit.new()
    name.text = String(c.name)
    name.placeholder_text = "Name"
    name.custom_minimum_size = Vector2(0,58)
    card.add_child(name)
    var mode := OptionButton.new()
    var modes := ["Pregnancy","Baby","Toddler","Kid","Teen"]
    for i in range(modes.size()):
        mode.add_item(modes[i])
        if modes[i] == String(c.mode):
            mode.select(i)
    mode.custom_minimum_size = Vector2(0,58)
    card.add_child(mode)
    var birthday := LineEdit.new()
    birthday.text = String(c.birthday)
    birthday.placeholder_text = "Birthday YYYY-MM-DD"
    birthday.custom_minimum_size = Vector2(0,58)
    card.add_child(birthday)
    card.add_child(_button("Save profile", Callable(self,"_save_profile").bind(name,mode,birthday), true))
    card.add_child(_button("Add another child", Callable(self,"_open_add_child")))

    var settings := _card("Settings", "")
    var gp := CheckButton.new()
    gp.text = "Grandparent read-only"
    gp.button_pressed = bool(state.settings.get("grandparent",false))
    gp.add_theme_font_size_override("font_size",20)
    gp.toggled.connect(_toggle_grandparent)
    settings.add_child(gp)

func _save_profile(name: LineEdit, mode: OptionButton, birthday: LineEdit) -> void:
    var c := _selected_child()
    c.name = name.text.strip_edges() if name.text.strip_edges() != "" else "Child"
    c.mode = mode.get_item_text(mode.selected)
    c.birthday = birthday.text.strip_edges()
    _save_state()
    _refresh_child_selector()
    refresh_current()
    _toast("Profile saved.")

func _open_add_child() -> void:
    var box := VBoxContainer.new()
    var name := LineEdit.new()
    name.placeholder_text = "Child name"
    name.custom_minimum_size = Vector2(0,58)
    box.add_child(name)
    var mode := OptionButton.new()
    for item in ["Pregnancy","Baby","Toddler","Kid","Teen"]:
        mode.add_item(item)
    mode.custom_minimum_size = Vector2(0,58)
    box.add_child(mode)
    box.add_child(_button("Add child", Callable(self,"_save_new_child").bind(name,mode), true))
    _show_modal("Add child", box)

func _save_new_child(name: LineEdit, mode: OptionButton) -> void:
    if name.text.strip_edges() == "":
        return
    var id := _uid("child")
    state.children.append({"id":id,"name":name.text.strip_edges(),"mode":mode.get_item_text(mode.selected),"birthday":"","memories":[],"feeds":[],"solids":[],"teeth":{},"milestones":{},"sleep":[]})
    state.selected = id
    _save_state()
    _hide_modal()
    _refresh_child_selector()
    refresh_current()

func _build_backup() -> void:
    _section_title("Backup / import")
    var card := _card("Portable JSON backup", "Copy your data to another phone or keep a manual backup.")
    var text := TextEdit.new()
    text.custom_minimum_size = Vector2(0,260)
    text.placeholder_text = "Export appears here, or paste backup JSON here to import."
    card.add_child(text)
    card.add_child(_button("Export + copy", Callable(self,"_export_backup").bind(text), true))
    card.add_child(_button("Import from box", Callable(self,"_import_backup").bind(text)))

func _export_backup(text: TextEdit) -> void:
    text.text = JSON.stringify(state,"  ")
    DisplayServer.clipboard_set(text.text)
    _toast("Backup copied.")

func _import_backup(text: TextEdit) -> void:
    var parsed = JSON.parse_string(text.text)
    if typeof(parsed) != TYPE_DICTIONARY:
        _toast("That doesn't look like a SproutBook backup.")
        return
    state = parsed
    _ensure_state()
    _save_state()
    _refresh_child_selector()
    show_page("home")
    _toast("Backup imported.")

func _readonly_guard() -> bool:
    if bool(state.settings.get("grandparent",false)):
        _toast("Grandparent mode is read-only.")
        return true
    return false

func _show_modal(title: String, body: Control) -> void:
    _hide_modal()
    modal_layer.mouse_filter = Control.MOUSE_FILTER_STOP
    var dim := ColorRect.new()
    dim.color = Color(0,0,0,0.66)
    dim.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    dim.mouse_filter = Control.MOUSE_FILTER_STOP
    modal_layer.add_child(dim)
    var center := CenterContainer.new()
    center.set_anchors_and_offsets_preset(Control.PRESET_FULL_RECT)
    center.mouse_filter = Control.MOUSE_FILTER_IGNORE
    modal_layer.add_child(center)
    var panel := PanelContainer.new()
    panel.custom_minimum_size = Vector2(620,0)
    panel.add_theme_stylebox_override("panel", _style(Color(0.045,0.085,0.065,1),28,ACCENT,2))
    center.add_child(panel)
    var box := VBoxContainer.new()
    box.add_theme_constant_override("separation",12)
    panel.add_child(box)
    var header := HBoxContainer.new()
    box.add_child(header)
    var label := _label(title,28,TEXT)
    label.size_flags_horizontal = Control.SIZE_EXPAND_FILL
    header.add_child(label)
    header.add_child(_button("×", Callable(self,"_hide_modal")))
    box.add_child(body)

func _hide_modal() -> void:
    for node in modal_layer.get_children():
        node.queue_free()
    modal_layer.mouse_filter = Control.MOUSE_FILTER_IGNORE

func _toast(value: String) -> void:
    var panel := PanelContainer.new()
    panel.z_index = 100
    panel.add_theme_stylebox_override("panel", _style(Color(0.02,0.03,0.025,0.96),18,ACCENT,1))
    panel.set_anchors_preset(Control.PRESET_CENTER_BOTTOM)
    panel.position = Vector2(70,-150)
    panel.size = Vector2(580,70)
    var l := _label(value,18,TEXT)
    l.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
    l.vertical_alignment = VERTICAL_ALIGNMENT_CENTER
    panel.add_child(l)
    add_child(panel)
    var tween := create_tween()
    tween.tween_interval(2.0)
    tween.tween_property(panel,"modulate:a",0.0,0.35)
    tween.tween_callback(panel.queue_free)

func _set_calendar_today() -> void:
    var d := Time.get_date_dict_from_system()
    calendar_year = int(d.year)
    calendar_month = int(d.month)
    selected_calendar_date = _today_string()

func _calendar_prev() -> void:
    calendar_month -= 1
    if calendar_month < 1:
        calendar_month = 12
        calendar_year -= 1
    refresh_current()

func _calendar_next() -> void:
    calendar_month += 1
    if calendar_month > 12:
        calendar_month = 1
        calendar_year += 1
    refresh_current()

func _days_in_month(year: int, month: int) -> int:
    if month in [1,3,5,7,8,10,12]:
        return 31
    if month in [4,6,9,11]:
        return 30
    var leap := (year % 4 == 0 and year % 100 != 0) or year % 400 == 0
    return 29 if leap else 28

func _weekday(year: int, month: int, day: int) -> int:
    var unix := Time.get_unix_time_from_datetime({"year":year,"month":month,"day":day,"hour":0,"minute":0,"second":0})
    return int(Time.get_datetime_dict_from_unix_time(unix).weekday)

func _month_name(month: int) -> String:
    return ["","January","February","March","April","May","June","July","August","September","October","November","December"][month]

func _date_string(year: int, month: int, day: int) -> String:
    return "%04d-%02d-%02d" % [year,month,day]

func _today_string() -> String:
    var d := Time.get_date_dict_from_system()
    return _date_string(int(d.year),int(d.month),int(d.day))

func _now_string() -> String:
    var d := Time.get_datetime_dict_from_system()
    return "%04d-%02d-%02d %02d:%02d" % [d.year,d.month,d.day,d.hour,d.minute]

func _birthday_matches(child: Dictionary, date: String) -> bool:
    var birthday := String(child.get("birthday",""))
    return birthday.length() >= 10 and date.length() >= 10 and birthday.substr(5,5) == date.substr(5,5)

func _day_mark_count(date: String) -> int:
    var count := 0
    for a in state.appointments:
        if String(a.get("date","")) == date:
            count += 1
    for c in state.children:
        if _birthday_matches(c,date):
            count += 1
    return count

func _uid(prefix: String) -> String:
    return prefix + "_" + str(int(Time.get_unix_time_from_system() * 1000.0)) + "_" + str(randi_range(1000,9999))

func _milestones_for_mode(mode: String) -> Array:
    match mode:
        "Baby":
            return ["Smiles socially","Holds head steadier","Rolls over","Reaches for toys","Sits with support","Babbles","Moves objects hand to hand","Pulls to stand"]
        "Toddler":
            return ["Walks independently","Uses several words","Points to show interest","Stacks blocks","Uses spoon with help","Runs","Combines two words","Pretend play"]
        "Kid":
            return ["Dresses with little help","Follows multi-step directions","Tells stories","Writes or draws intentionally","Takes turns","Helps with simple chores"]
        "Teen":
            return ["Manages more responsibilities","Develops independent interests","Plans longer projects","Builds stronger peer relationships","Practices self-advocacy"]
        _:
            return ["Prenatal appointment","Movement noticed","Sleep space ready","Hospital bag ready"]

func _calm_steps(problem: String) -> Array:
    match problem:
        "Baby won't stop crying":
            return ["Check hunger, diaper, temperature and obvious pain.","Lower stimulation: dim lights, hold close, use steady shushing or white noise and gentle sway.","If you're overloaded, place baby safely in the crib and take a short reset.","Persistent or unusual crying with fever, breathing trouble, poor responsiveness, dehydration signs or something that feels seriously wrong needs medical assessment."]
        "Baby won't latch":
            return ["Reset positioning: belly-to-belly, nose near nipple, wait for a wide mouth.","Bring baby in close with chin touching first; try cross-cradle, football or laid-back position.","If it pinches, break suction with a clean finger and relatch rather than pushing through pain.","Ongoing painful latch, poor milk transfer, fewer wet diapers or weight concerns are good reasons to contact a lactation professional or clinician."]
        "Gas or spit-up":
            return ["Pause feeding and burp gently.","Keep baby upright for a while after feeding.","Try smaller, paced feeds and avoid pressure on the belly.","Forceful or green vomit, blood, dehydration or poor weight gain should be medically assessed."]
        "Fever / seems unwell":
            return ["Check temperature with a reliable thermometer.","Keep baby comfortable and offer normal feeds or fluids as appropriate.","Write down temperature, symptoms, wet diapers and when it started.","Young infants with fever and any child with trouble breathing, severe lethargy, seizure, blue or gray color, dehydration or rapidly worsening symptoms need urgent medical care."]
        "I'm overwhelmed":
            return ["Put baby somewhere safe such as the crib.","Step away for a few minutes, drink water and slow your breathing.","Tag another trusted adult in if one is available.","Never shake a baby. If you feel you may lose control, keep baby safe and get immediate support from someone you trust or emergency services if needed."]
        _:
            return ["Call emergency services now if the child cannot breathe, cry or cough effectively.","Use age-appropriate choking first aid if you know it; do not blindly sweep the mouth.","Even if the object comes out, seek medical advice if breathing remains abnormal or there was loss of consciousness.","A choking or breathing emergency is not a situation to troubleshoot in-app — get emergency help immediately."]
