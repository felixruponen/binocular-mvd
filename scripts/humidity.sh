
adb shell am broadcast -a cc.arduino.mvd.services.actions.CLEAR_ROUTES


adb shell am broadcast -a cc.arduino.mvd.services.actions.ADD_BEAN --es cc.arduino.mvd.services.extras.MAC "D0:39:72:D3:4D:BD"

#adb shell am broadcast -a cc.arduino.mvd.services.actions.CREATE_BINDING --es cc.arduino.mvd.helper.extras.BINDING_NAME "Test Binding" --es cc.arduino.mvd.services.extras.MAC "B4:99:4C:1E:C5:1C" --es cc.arduino.mvd.services.extras.NAME "BinocularService" --es cc.arduino.mvd.helper.extras.CODE "5528e947d7ad015decb3af13" --es cc.arduino.mvd.helper.extras.PIN "0"

#adb shell am broadcast -a cc.arduino.mvd.services.actions.CREATE_BINDING --es cc.arduino.mvd.helper.extras.BINDING_NAME "Test Binding" --es cc.arduino.mvd.services.extras.MAC "B4:99:4C:1E:C5:1C" --es cc.arduino.mvd.services.extras.NAME "BinocularService" --es cc.arduino.mvd.helper.extras.CODE "552929cf306eeede84662bc8" --es cc.arduino.mvd.helper.extras.PIN "2"

adb shell am broadcast -a cc.arduino.mvd.services.actions.ADD_ROUTE --es cc.arduino.mvd.services.extras.NAMES "BeanService, BinocularService"
