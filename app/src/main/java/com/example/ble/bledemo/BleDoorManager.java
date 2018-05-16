package com.example.ble.bledemo;

import android.app.Application;
import android.bluetooth.BluetoothGatt;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;

import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanAndConnectCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;
import com.clj.fastble.utils.HexUtil;

import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import static com.example.ble.bledemo.BleConfigConstant.DOOR_OPEN_ERROR;
import static com.example.ble.bledemo.BleConfigConstant.DOOR_OPEN_SUCCESS;
import static com.example.ble.bledemo.BleConfigConstant.NOTIFY_UUID;
import static com.example.ble.bledemo.BleConfigConstant.PROTOCOL_PACKAGE_HEAD;
import static com.example.ble.bledemo.BleConfigConstant.SERVICE_UUID;
import static com.example.ble.bledemo.BleConfigConstant.WRITE_UUID;

/**
 * 蓝牙开门辅助类
 * Created by guozhen.hou on 2018/2/6.
 */

public class BleDoorManager {
    private final static int OperateTime = 4 * 1000;//设置操作超时时间，默认5秒
    private final static int ScanTimeOut = 4 * 1000;//设置扫描超时时间，默认5秒；同时该时间也是用于不间断扫描的时长

    private String openResult;


    private static final int MAX_CONNECT_COUNT=3;//最大连接次数
    private BleDevice bleDevice;

    private RefreshListener refreshListener;

    public BleDoorManager(RefreshListener refreshListener){
        this.refreshListener=refreshListener;
    }

    /**
     * 初始化操作
     *
     * @param application application对象
     */
    public static void init(Application application) {
        BleManager.getInstance().init(application);//初始化
        BleManager.getInstance().enableLog(true)
                                .setReConnectCount(MAX_CONNECT_COUNT,500)
                                .setOperateTimeout(OperateTime);
        initScanRule(new String[]{SERVICE_UUID});
    }

    /**
     * 开始扫描
     * 扫描的结构通过aac的model回调给使用页面的观察者处理，此处只需要在扫描到对应结果后，更新model中的数据
     * 如果处于链接周期中，不进行扫描
     *
     */
    public void startScanAndConnect() {
        if(!BleManager.getInstance().isBlueEnable()) BleManager.getInstance().enableBluetooth();
        if(bleDevice!=null) {
            if (BleManager.getInstance().isConnected(bleDevice)) return;//如果处于当前设备的链接过程中，不予再次连接
            BleManager.getInstance().disconnectAllDevice();//断开所有设备的链接操作
            if (BleManager.getInstance().getBluetoothGatt(bleDevice) != null)
                BleManager.getInstance().getBluetoothGatt(bleDevice).close();
        }
        BleManager.getInstance().cancelScan();
        BleManager.getInstance().scanAndConnect(new BleScanAndConnectCallback(){
            @Override
            public void onStartConnect() {
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                // 连接失败
                refreshListener.completeOneCycleProcess(DOOR_OPEN_ERROR);
            }

            @Override
            public void onConnectSuccess(final BleDevice bleDevice, BluetoothGatt gatt, int status) {
                // 连接成功，BleDevice即为所连接的BLE设备
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        notifyBleInfo(bleDevice);//蓝牙开门进行校验
                    }
                }, 100);
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                // 连接中断，isActiveDisConnected表示是否是主动调用了断开连接方法
                refreshListener.completeOneCycleProcess(openResult);
            }

            @Override
            public void onScanStarted(boolean success) {

            }

            @Override
            public void onScanFinished(BleDevice scanResult) {
                bleDevice=scanResult;
            }
        });
    }

    private void notifyBleInfo(final BleDevice bleDevice) {
        Log.d("ble", "notify");
        BleManager.getInstance().notify(
                bleDevice,
                SERVICE_UUID,
                NOTIFY_UUID,
                new BleNotifyCallback() {

                    @Override
                    public void onNotifySuccess() {

                    }

                    @Override
                    public void onNotifyFailure(BleException e) {//获取通知失败
                    }

                    @Override
                    public void onCharacteristicChanged(byte[] bytes) {
                        // 打开通知操作成功
                        try {
                            final String receiveNotify = new String(bytes);
                            Log.d("receiveNotify",receiveNotify);
                            if (DOOR_OPEN_SUCCESS.equalsIgnoreCase(receiveNotify) || DOOR_OPEN_ERROR.equalsIgnoreCase(receiveNotify)) {
                                openResult = receiveNotify;
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        refreshListener.completeOneCycleProcess(receiveNotify);
                                    }
                                });
                                return;
                            }
                            //12位随机值,例如 000011472672
                            if (receiveNotify.length() > 1) {
                                String enStr=HexUtil.formatHexString(bleDevice.getScanRecord()).substring(14,34);
                                Log.d("ble",("Address:"+bleDevice.getDevice().getAddress()+"name:"+bleDevice.getDevice().getName()+"MAC"+bleDevice.getMac()+"key"+bleDevice.getKey()+"getScanRecord"+ HexUtil.formatHexString(bleDevice.getScanRecord())));
                                String aesStr = new StringBuilder()
                                        .append(PROTOCOL_PACKAGE_HEAD)
                                        .append("REMAIN_BLUETOOTH")
                                        .append(encrypt(receiveNotify + enStr, "REMAIN_BLUETOOTH"))
                                        .substring(0, 76);
                                writeBlueInfo(bleDevice, aesStr.getBytes());
                            }

                        } catch (Exception e) {
                            refreshListener.completeOneCycleProcess(openResult);
                            e.printStackTrace();
                        }
                    }
                });
    }

    public static final String bytesToHexString(byte[] bArray) {
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    /**
     * aes 加密
     *
     * @param sSrc 原文
     * @param sKey key
     * @return 密文
     */
    private String encrypt(String sSrc, String sKey) {
        if (sKey == null) {
            return "";
        }
        // 判断Key是否为16位
        if (sKey.length() != 16) {
            return "";
        }
        try {
            byte[] raw = sKey.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");//"算法/模式/补码方式"
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
            return Base64.encodeToString(encrypted, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
        //此处使用BASE64做转码功能，同时能起到2次加密的作用。
    }

    /**
     * 蓝牙开门核心校验，门禁数据传输功能
     *
     * @param bleDevice 蓝牙信息
     * @param data      传输数据
     *                  在没有扩大MTU及扩大MTU无效的情况下，当遇到超过20字节的长数据需要发送的时候，需要进行分包。
     *                  参数boolean split表示是否使用分包发送；无boolean split参数的write方法默认对超过20字节的数据进行分包发送。
     *                  关于onWriteSuccess回调方法: current表示当前发送第几包数据，total表示本次总共多少包数据，justWrite表示刚刚发送成功的数据包。
     */
    private void writeBlueInfo(BleDevice bleDevice, byte[] data) {
        BleManager.getInstance().write(
                bleDevice,
                SERVICE_UUID,
                WRITE_UUID,
                data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        // 发送数据到设备成功
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        // 发送数据到设备失败
                    }
                });
    }

    private static void initScanRule(String[] uuids) {
        UUID[] serviceUuids = null;
        if (uuids != null && uuids.length > 0) {
            serviceUuids = new UUID[uuids.length];
            for (int i = 0; i < uuids.length; i++) {
                String name = uuids[i];
                String[] components = name.split("-");
                if (components.length != 5) {
                    serviceUuids[i] = null;
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i]);
                }
            }
        }

        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(serviceUuids)      // 只扫描指定的服务的设备，可选
//                .setDeviceName(true, names)   // 只扫描指定广播名的设备，可选
//                .setDeviceMac(mac)                  // 只扫描指定mac的设备，可选
//                .setAutoConnect(true)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(ScanTimeOut)              // 扫描超时时间，可选，默认10秒,<0表示不限制扫描时间
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }


}
