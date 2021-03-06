package com.androidex.capbox.service;

import android.content.Intent;

import com.androidex.boxlib.modules.LocationModules;
import com.androidex.boxlib.modules.ServiceBean;
import com.androidex.boxlib.service.BleService;
import com.androidex.capbox.MyApplication;
import com.androidex.capbox.data.cache.SharedPreTool;
import com.androidex.capbox.db.DaoSession;
import com.androidex.capbox.db.Note;
import com.androidex.capbox.db.NoteDao;
import com.androidex.capbox.ui.activity.LockScreenActivity;
import com.androidex.capbox.utils.RLog;
import com.androidex.capbox.utils.SystemUtil;

import java.util.List;

import static com.androidex.boxlib.utils.BleConstants.BLE.BLE_CONN_DIS;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_ADDRESS;
import static com.androidex.boxlib.utils.BleConstants.BLECONSTANTS.BLECONSTANTS_ISACTIVEDisConnect;
import static com.androidex.capbox.utils.Constants.BASE.ACTION_RSSI_IN;
import static com.androidex.capbox.utils.Constants.BASE.ACTION_RSSI_OUT;
import static com.androidex.capbox.utils.Constants.BASE.ACTION_TEMP_OUT;
import static com.androidex.capbox.utils.Constants.SP.SP_DISTANCE_TYPE;
import static com.androidex.capbox.utils.Constants.SP.SP_LOST_TYPE;
import static com.androidex.capbox.utils.Constants.SP.SP_TEMP_TYPE;
import static com.baidu.mapapi.BMapManager.getContext;

/**
 * @author liyp
 * 应用的服务类
 * @editTime 2017/12/4
 */

public class MyBleService extends BleService {
    public static final String TAG = "MyBleService";
    private static MyBleService service;

    @Override
    public void onCreate() {
        super.onCreate();
        RLog.e("MyBleService 启动");
        setLockScreenActivity(LockScreenActivity.class);//设置锁屏界面的Activity
        initDB();
    }

    /**
     * @return
     */
    public static BleService getInstance() {
        if (service == null) {
            service = new MyBleService();
        }
        return service;
    }

    private static NoteDao noteDao;

    public void initDB() {
        DaoSession daoSession = ((MyApplication) getApplication()).getDaoSession();
        noteDao = daoSession.getNoteDao();
    }

    /**
     * -keep class org.greenrobot.greendao.**{*;}
     * -keep public interface org.greenrobot.greendao.**
     * -keep class org.greenrobot.greendao.database.Database
     * -keepclassmembers class * extends org.greenrobot.greendao.AbstractDao {
     * public static java.lang.String TABLENAME;
     * }
     * -keep class **$Properties
     * -keep class net.sqlcipher.database.**{*;}
     * -keep public interface net.sqlcipher.database.**
     * -dontwarn net.sqlcipher.database.**
     * -dontwarn org.greenrobot.greendao.**
     * 插入定位数据到数据库
     * <p>
     * 插入数据到数据库Note{id=1, address='A4:34:F1:84:25:29', time=1526025586, lat='2237.601437N', lon='11404.916731E', alt='116.0M', type=null}
     *
     * @param address
     * @param modules
     * @param longTime
     */
    @Override
    protected void insertLocData(String address, LocationModules modules, long longTime) {
        Note note = new Note();
        note.setAddress(address);
        note.setLat(modules.getLat());
        note.setLon(modules.getLon());
        note.setAlt(modules.getAlt());
        note.setTime(longTime);
        noteDao.insert(note);
        RLog.d("插入数据到数据库" + note.toString());
    }

    /**
     * 根据mac地址查询设备数据
     * @param address
     * @return
     */
    public static List<Note> getLocListData(String address) {
        RLog.d("开始读取数据库数据");
        //notesQuery = noteDao.queryBuilder().orderAsc(NoteDao.Properties.Address).build();
        return noteDao.queryBuilder().where(NoteDao.Properties.Address.eq(address))
                .orderAsc(NoteDao.Properties.Time).list();
    }

    /**
     * 异常断开后调用，主动断开需要调用以下方法可不收到断开信息，不必做处理。
     * ServiceBean device = getConnectDevice(address);
     * if (device != null) {
     * device.setActiveDisConnect(true);
     * }
     *
     * @param address
     */
    @Override
    public void disConnect(String address) {
        ServiceBean device = getConnectDevice(address);
        if (device != null) {
            device.setStopGetRssi();
            if (device.isActiveDisConnect()) {//判断是否是主动断开，true就不报警,在主动断开的时候就要设置该值为true
                sendDisconnectMessage(address, true);
            } else {
                sendDisconnectMessage(address, false);
            }
        } else {
            sendDisconnectMessage(address, false);
        }
    }

    /**
     * 连接上设备后调用
     *
     * @param address
     */
    @Override
    protected void initDevice(String address) {
        ServiceBean connectDevice = MyBleService.getInstance().getConnectDevice(address);
        if (connectDevice == null) return;
        ServiceBean device = SharedPreTool.getInstance(this).getObj(ServiceBean.class, address);
        if (device != null) {
            RLog.e("device====" + device.toString());
            connectDevice.setStartCarry(device.isStartCarry());//取出携行状态
            connectDevice.setPolice(device.isPolice());
            connectDevice.setDistanceAlarm(device.isDistanceAlarm());
            connectDevice.setTamperAlarm(device.isTamperAlarm());
            connectDevice.setTempAlarm(device.isTempAlarm());
            connectDevice.setHumAlarm(device.isHumAlarm());
            RLog.e("转换后设备参数" + connectDevice.toString());
        } else {
            //RLog.e("device ");
        }
    }

    /**
     * 断开连接时发送消息通知
     *
     * @param address
     * @param isActive
     */
    private void sendDisconnectMessage(String address, boolean isActive) {
        if (SharedPreTool.getInstance(this).getBoolData(SharedPreTool.IS_POLICE, true)) {
            ServiceBean device = SharedPreTool.getInstance(this).getObj(ServiceBean.class, address);
            if (device != null && device.isPolice()) {//断开连接广播
                sendBroadDis(address, isActive);
            } else {
                ServiceBean connectDevice = MyBleService.getInstance().getConnectDevice(address);
                if (connectDevice != null && connectDevice.isPolice()) {//断开连接广播
                    sendBroadDis(address, isActive);
                } else {
                    RLog.e("已关闭单个箱子报警开关");
                }
            }
        } else {
            RLog.e("已关闭报警开关");
        }
    }

    /**
     * 发送蓝牙脱距广播
     *
     * @param address
     * @param isActive
     */
    public void sendBroadDis(String address, boolean isActive) {
        if (!isStartCarry(address)) {
            sendDisBroadcast(address, true);
            return;//判断是否启动携行，没启动携行时不报警
        }
        switch (SharedPreTool.getInstance(this).getIntData(SP_LOST_TYPE, 0)) {
            case 0:
                sendDisBroadcast(address, isActive);
                if (!isActive) {
                    SystemUtil.startPlayerRaw(getContext());
                }
                break;
            case 1:
                if (!isActive) {
                    SystemUtil.startPlayerRaw(getContext());
                }
                sendDisBroadcast(address, true);
                break;
            case 2:
                sendDisBroadcast(address, isActive);
                break;
            default:
                break;
        }
    }

    /**
     * @param address
     * @return
     */
    public boolean isStartCarry(String address) {
        ServiceBean obj = SharedPreTool.getInstance(this).getObj(ServiceBean.class, address);
        if (obj != null) {
            RLog.e("service is startCarry ==" + obj.isStartCarry());
            if (obj.isStartCarry()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private void sendDisBroadcast(String address, boolean isActive) {
        Intent intent = new Intent(BLE_CONN_DIS);
        intent.putExtra(BLECONSTANTS_ADDRESS, address);
        intent.putExtra(BLECONSTANTS_ISACTIVEDisConnect, isActive);
        sendBroadcast(intent);
    }


    /**
     * 连续三次超出阈值会回调这里，1s更新一次信号强度
     */
    @Override
    public void outOfScopeRssi(String address) {
        if (SharedPreTool.getInstance(this).getBoolData(SharedPreTool.IS_POLICE, true)) {
            ServiceBean device = SharedPreTool.getInstance(this).getObj(ServiceBean.class, address);
            if (device != null && device.isDistanceAlarm()) {//信号弱，报警
                sendRSSIOut(address);
            } else {
                ServiceBean connectDevice = MyBleService.getInstance().getConnectDevice(address);
                if (connectDevice != null && connectDevice.isDistanceAlarm()) {//信号弱，报警
                    sendRSSIOut(address);
                } else {
                    RLog.e("已关闭单个箱子距离报警开关");
                }
            }
        } else {
            RLog.e("已关闭报警开关");
        }
    }

    /**
     * 发送信号弱的广播
     *
     * @param address
     */
    private void sendRSSIOut(String address) {
        if (!isStartCarry(address)) return;//判断是否启动携行，没启动携行时不报警
        switch (SharedPreTool.getInstance(this).getIntData(SP_DISTANCE_TYPE, 0)) {
            case 0:
                sendTempOutBroad(address, ACTION_RSSI_OUT);
                SystemUtil.startPlayerRaw(getContext());
                break;
            case 1:
                SystemUtil.startPlayerRaw(getContext());
                break;
            case 2:
                sendTempOutBroad(address, ACTION_RSSI_OUT);
                break;
            default:
                break;
        }

    }

    /**
     * 超出阈值调用报警后，又恢复到阈值范围内，停止报警回调
     */
    @Override
    public void inOfScopeRssi(String address) {
        sendTempOutBroad(address, ACTION_RSSI_IN);
        SystemUtil.stopPlayRaw();
    }

    /**
     * 温度超范围报警
     */
    @Override
    public void outOfScopeTempPolice(String address) {
        if (SharedPreTool.getInstance(this).getBoolData(SharedPreTool.IS_POLICE, true)) {
            ServiceBean device = SharedPreTool.getInstance(this).getObj(ServiceBean.class, address);
            if (device != null && device.isTempAlarm()) {//非主动断开时，报警
                RLog.e("发送广播，温度超范围报警");
                sendTempOutBroadcast(address);
            } else {
                ServiceBean connectDevice = MyBleService.getInstance().getConnectDevice(address);
                if (connectDevice != null && connectDevice.isTempAlarm()) {//非主动断开时，报警
                    RLog.e("发送广播，温度超范围报警");
                    sendTempOutBroadcast(address);
                } else {
                    RLog.e("已关闭单个箱子温度报警开关");
                }
            }
        } else {
            RLog.e("已关闭报警开关");
        }
    }

    /**
     * 发送温度超范围广播
     *
     * @param address
     */
    private void sendTempOutBroadcast(String address) {
        if (!isStartCarry(address)) return;//判断是否启动携行，没启动携行时不报警
        switch (SharedPreTool.getInstance(this).getIntData(SP_TEMP_TYPE, 0)) {
            case 0:
                sendTempOutBroad(address, ACTION_TEMP_OUT);
                SystemUtil.startPlayerRaw(getContext());
                break;
            case 1:
                SystemUtil.startPlayerRaw(getContext());
                break;
            case 2:
                sendTempOutBroad(address, ACTION_TEMP_OUT);
                break;
            default:
                break;
        }
    }

    private void sendTempOutBroad(String address, String actionTempOut) {
        Intent intent = new Intent(actionTempOut);
        intent.putExtra(BLECONSTANTS_ADDRESS, address);
        sendBroadcast(intent);
    }

    /**
     * 温度又恢复到范围内
     */
    @Override
    public void inOfScopeTempPolice(String address) {
        SystemUtil.stopPlayRaw();
    }

}
