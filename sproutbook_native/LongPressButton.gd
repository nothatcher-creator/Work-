extends Button
signal long_pressed

@export var hold_seconds := 0.45
var held := false
var fired := false
var started_at := 0

func _ready() -> void:
    button_down.connect(_down)
    button_up.connect(_up)
    set_process(true)

func _down() -> void:
    held = true
    fired = false
    started_at = Time.get_ticks_msec()

func _up() -> void:
    held = false

func _process(_delta: float) -> void:
    if held and not fired and Time.get_ticks_msec() - started_at >= int(hold_seconds * 1000.0):
        fired = true
        held = false
        long_pressed.emit()
