
adb shell am broadcast -a cc.arduino.mvd.services.actions.CLEAR_ROUTES
adb shell am broadcast -a cc.arduino.mvd.services.actions.CLEAR_BINDINGS


adb shell am broadcast -a cc.arduino.mvd.services.actions.REMOVE_BEAN --es cc.arduino.mvd.services.extras.MAC "B4:99:4C:1E:C5:1C"

adb shell am broadcast -a cc.arduino.mvd.services.actions.REMOVE_BEAN --es cc.arduino.mvd.services.extras.MAC "D0:39:72:D3:4D:BD"


adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE --es cc.arduino.mvd.services.extras.NAME "BeanService"

adb shell am broadcast -a cc.arduino.mvd.services.actions.STOP_SERVICE --es cc.arduino.mvd.services.extras.NAME "BinocularService"

