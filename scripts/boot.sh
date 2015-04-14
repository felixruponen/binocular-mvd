adb shell am broadcast -a cc.arduino.mvd.helper.actions.ENABLE_DEBUG --ez cc.arduino.mvd.helper.extras.PERSIST true

adb shell am broadcast -a cc.arduino.mvd.helper.actions.SET_CONFIGURATION --es cc.arduino.mvd.helper.extras.ID "BiSmart"

adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE --es cc.arduino.mvd.services.extras.NAME "BeanService" --ei cc.arduino.mvd.services.extras.DELAY 500

adb shell am broadcast -a cc.arduino.mvd.helper.actions.SCAN --ei cc.arduino.mvd.services.extras.TIMEOUT 5000

sleep 2

adb shell am broadcast -a cc.arduino.mvd.services.actions.START_SERVICE --es cc.arduino.mvd.services.extras.NAME "BinocularService" --es cc.arduino.mvd.services.extras.URL "http://188.226.207.177/" --ei cc.arduino.mvd.services.extras.DELAY 10000


