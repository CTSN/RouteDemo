package com.roy.routedemo;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.Polyline;
import com.amap.api.navi.AMapNavi;
import com.amap.api.navi.AMapNaviListener;
import com.amap.api.navi.model.AMapCarInfo;
import com.amap.api.navi.model.AMapLaneInfo;
import com.amap.api.navi.model.AMapModelCross;
import com.amap.api.navi.model.AMapNaviCameraInfo;
import com.amap.api.navi.model.AMapNaviCross;
import com.amap.api.navi.model.AMapNaviInfo;
import com.amap.api.navi.model.AMapNaviLocation;
import com.amap.api.navi.model.AMapNaviPath;
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo;
import com.amap.api.navi.model.AMapRestrictionInfo;
import com.amap.api.navi.model.AMapServiceAreaInfo;
import com.amap.api.navi.model.AimLessModeCongestionInfo;
import com.amap.api.navi.model.AimLessModeStat;
import com.amap.api.navi.model.NaviInfo;
import com.amap.api.navi.model.NaviLatLng;
import com.amap.api.navi.view.RouteOverLay;
import com.autonavi.tbt.TrafficFacilityInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RestRouteShowActivity extends AppCompatActivity implements AMapNaviListener, OnClickListener, AMap.OnPolylineClickListener {
    /**
     * to_receive_address
     */
    private TextView mTvTitle;
    /**
     * 收货地
     */
    private TextView mTvFrom;
    /**
     * 目的地
     */
    private TextView mTvTo;
    private ImageView mIvCall;
    /**
     * 导航对象(单例)
     */
    private AMapNavi mAMapNavi;
    private AMap mAmap;
    /**
     * 地图对象
     */
    private MapView mRouteMapView;
    private Marker mStartMarker;
    private Marker mEndMarker;
    private NaviLatLng endLatlng = new NaviLatLng(39.955846, 116.352765);
    private NaviLatLng startLatlng = new NaviLatLng(39.925041, 116.437901);
    private List<NaviLatLng> startList = new ArrayList<NaviLatLng>();
    /**
     * 途径点坐标集合
     */
    private List<NaviLatLng> wayList = new ArrayList<NaviLatLng>();
    /**
     * 终点坐标集合［建议就一个终点］
     */
    private List<NaviLatLng> endList = new ArrayList<NaviLatLng>();
    /**
     * 保存当前算好的路线
     */
    private SparseArray<RouteOverLay> routeOverlays = new SparseArray<RouteOverLay>();

    /**
     * 当前用户选中的路线，在下个页面进行导航
     */
    private int routeIndex;
    /**
     * 路线的权值，重合路线情况下，权值高的路线会覆盖权值低的路线
     **/
    private int zindex = 1;
    /**
     * 路线计算成功标志位
     */
    private boolean calculateSuccess = false;
    private boolean chooseRouteSuccess = false;
    private LatLngBounds.Builder boundsBuilder = LatLngBounds.builder();
    private RelativeLayout mMain;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_action);
        getSupportActionBar().hide();
        initView(savedInstanceState);
    }


    protected void initView(Bundle savedInstanceState) {
        mRouteMapView = (MapView) findViewById(R.id.map);
        mRouteMapView.onCreate(savedInstanceState);
        mAmap = mRouteMapView.getMap();
        mAmap.getUiSettings().setZoomControlsEnabled(false);
        mAmap.setOnPolylineClickListener(this);
        mAmap.setMapType(AMap.MAP_TYPE_NAVI);
        try {
            mAMapNavi = AMapNavi.getInstance(getApplicationContext());
            if (mAMapNavi != null) {
                mAMapNavi.addAMapNaviListener(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        mTvTitle = (TextView) findViewById(R.id.tv_title);
        mTvFrom = (TextView) findViewById(R.id.tv_from);
        mTvTo = (TextView) findViewById(R.id.tv_to);
        mIvCall = (ImageView) findViewById(R.id.iv_call);
        mIvCall.setOnClickListener(this);
        mMain = (RelativeLayout) findViewById(R.id.main);
        mMain.setVisibility(View.VISIBLE);
        loadData();
    }


    private void loadData() {

        startLatlng = new NaviLatLng(23.142211, 113.32476);
        startList.clear();
        startList.add(startLatlng);
        endLatlng = new NaviLatLng( 23.125524, 113.264081);
        endList.clear();
        endList.add(endLatlng);
        caculateRoute();

        boundsBuilder.include(new LatLng(23.125524, 23.125524));
        mRouteMapView.post(new Runnable() {
            @Override
            public void run() {
                mAmap.animateCamera(CameraUpdateFactory
                        .newLatLngBounds(boundsBuilder.build(), 14));
            }
        });
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mRouteMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mRouteMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mRouteMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        startList.clear();
        wayList.clear();
        endList.clear();
        routeOverlays.clear();
        mRouteMapView.onDestroy();
        /**
         * 当前页面只是展示地图，activity销毁后不需要再回调导航的状态
         */
        mAMapNavi.removeAMapNaviListener(this);
        mAMapNavi.destroy();
    }

    @Override
    public void onInitNaviSuccess() {
    }

    @Override
    public void onCalculateRouteSuccess(int[] ints) {
        //清空上次计算的路径列表。
        routeOverlays.clear();
        HashMap<Integer, AMapNaviPath> paths = mAMapNavi.getNaviPaths();
        for (int i = 0; i < ints.length; i++) {
            AMapNaviPath path = paths.get(ints[i]);
            if (path != null) {
                drawRoutes(ints[i], path);
            }
        }
        changeRoute();
        mMain.setVisibility(View.VISIBLE);
    }

    @Override
    public void onCalculateRouteFailure(int arg0) {
        calculateSuccess = false;
        Toast.makeText(getApplicationContext(), "计算路线失败，errorcode＝" + arg0, Toast.LENGTH_SHORT).show();
    }

    private void drawRoutes(int routeId, AMapNaviPath path) {
        calculateSuccess = true;
        RouteOverLay routeOverLay = new RouteOverLay(mAmap, path, this);
        routeOverLay.setTrafficLine(true);
        routeOverLay.setStartPointBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.icon_start));
        routeOverLay.setEndPointBitmap(BitmapFactory.decodeResource(getResources(), R.mipmap.icon_end));
        routeOverLay.addToMap();
        routeOverLay.zoomToSpan(120);
        routeOverlays.put(routeId, routeOverLay);
    }

    public void changeRoute() {
        if (!calculateSuccess) {
            Toast.makeText(this, "请先算路", Toast.LENGTH_SHORT).show();
            return;
        }
        /**
         * 计算出来的路径只有一条
         */
        if (routeOverlays.size() == 1) {
            chooseRouteSuccess = true;
            mAMapNavi.selectRouteId(routeOverlays.keyAt(0));
            return;
        }
        if (routeIndex >= routeOverlays.size())
            routeIndex = 0;
        int routeID = routeOverlays.keyAt(routeIndex);
        for (int i = 0; i < routeOverlays.size(); i++) {
            int key = routeOverlays.keyAt(i);
            routeOverlays.get(key).setTransparency(0.4f);
            routeOverlays.get(key).setZindex(0);
        }
        routeOverlays.get(routeID).setTransparency(1);
        /**把用户选择的那条路的权值弄高，使路线高亮显示的同时，重合路段不会变的透明**/
        routeOverlays.get(routeID).setZindex(1);

        mAMapNavi.selectRouteId(routeID);
        routeIndex++;
        chooseRouteSuccess = true;

        /**选完路径后判断路线是否是限行路线**/
        AMapRestrictionInfo info = mAMapNavi.getNaviPath().getRestrictionInfo();
        if (!TextUtils.isEmpty(info.getRestrictionTitle())) {
            if (routeIndex == 0) {
                return;
            }
            changeRoute();
        }
    }

    /**
     * 清除当前地图上算好的路线
     */
    private void clearRoute() {
        for (int i = 0; i < routeOverlays.size(); i++) {
            RouteOverLay routeOverlay = routeOverlays.valueAt(i);
            routeOverlay.setTrafficLine(true);
            routeOverlay.removeFromMap();
        }
        routeOverlays.clear();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
        }
    }

    public void caculateRoute() {
        clearRoute();
        int strategyFlag = 0;
        try {
            strategyFlag = mAMapNavi.strategyConvert(true, true, true, false, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (strategyFlag >= 0) {
            AMapCarInfo carInfo = new AMapCarInfo();
            //设置车牌
//                    carInfo.setCarNumber(carNumber);
            //设置车牌是否参与限行算路
//                    carInfo.setRestriction(true);
            mAMapNavi.setCarInfo(carInfo);
            mAMapNavi.calculateDriveRoute(startList, endList, wayList, strategyFlag);
        }
    }

    /**
     * ************************************************** 在算路页面，以下接口全不需要处理，在以后的版本中我们会进行优化***********************************************************************************************
     **/

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo arg0) {


    }

    @Override
    public void OnUpdateTrafficFacility(TrafficFacilityInfo arg0) {


    }

    @Override
    public void OnUpdateTrafficFacility(AMapNaviTrafficFacilityInfo[] arg0) {


    }

    @Override
    public void hideCross() {


    }

    @Override
    public void hideLaneInfo() {


    }

    @Override
    public void notifyParallelRoad(int arg0) {


    }

    @Override
    public void onArriveDestination() {


    }

    @Override
    public void onArrivedWayPoint(int arg0) {


    }

    @Override
    public void onEndEmulatorNavi() {


    }

    @Override
    public void onGetNavigationText(int arg0, String arg1) {


    }

    @Override
    public void onGetNavigationText(String s) {

    }

    @Override
    public void onGpsOpenStatus(boolean arg0) {


    }

    @Override
    public void onInitNaviFailure() {


    }

    @Override
    public void onLocationChange(AMapNaviLocation arg0) {


    }

    @Override
    public void onNaviInfoUpdate(NaviInfo arg0) {


    }

    @Override
    public void onNaviInfoUpdated(AMapNaviInfo arg0) {


    }

    @Override
    public void updateCameraInfo(AMapNaviCameraInfo[] aMapCameraInfos) {

    }

    @Override
    public void onServiceAreaUpdate(AMapServiceAreaInfo[] amapServiceAreaInfos) {

    }

    @Override
    public void onReCalculateRouteForTrafficJam() {


    }

    @Override
    public void onReCalculateRouteForYaw() {


    }

    @Override
    public void onStartNavi(int arg0) {


    }

    @Override
    public void onTrafficStatusUpdate() {


    }

    @Override
    public void showCross(AMapNaviCross arg0) {


    }

    @Override
    public void showLaneInfo(AMapLaneInfo[] arg0, byte[] arg1, byte[] arg2) {


    }

    @Override
    public void updateAimlessModeCongestionInfo(AimLessModeCongestionInfo arg0) {


    }

    @Override
    public void onPlayRing(int i) {

    }

    @Override
    public void updateAimlessModeStatistics(AimLessModeStat arg0) {


    }

    @Override
    public void showModeCross(AMapModelCross aMapModelCross) {

    }

    @Override
    public void hideModeCross() {

    }

    @Override
    public void updateIntervalCameraInfo(AMapNaviCameraInfo aMapNaviCameraInfo, AMapNaviCameraInfo aMapNaviCameraInfo1, int i) {

    }

    @Override
    public void showLaneInfo(AMapLaneInfo aMapLaneInfo) {

    }

    @Override
    public void onPolylineClick(Polyline polyline) {
        if (routeOverlays != null && routeOverlays.size() == 1){
            return;
        }
        List<LatLng> latLngs = polyline.getPoints();
        if (latLngs.size() == 0){
            return;
        }
        LatLng latLng = latLngs.get(0);
        outer:
        for (int i = 0; i < routeOverlays.size(); i++) {
            int key = routeOverlays.keyAt(i);
            List<NaviLatLng> naviLatLngs = routeOverlays.get(key).getAMapNaviPath().getCoordList();
            for (NaviLatLng naviLatLng : naviLatLngs) {
                if (Math.abs((naviLatLng.getLatitude() - latLng.latitude)) <= 0.000001
                        && Math.abs((naviLatLng.getLongitude()- latLng.longitude)) <= 0.00001){
                    if (i == routeIndex){   // 已经选过该路线 跳转下一条路线 可能重复路线
                        continue outer;
                    }
                    for (int j = 0; j < routeOverlays.size(); j++) {
                        if (i == j){    //选中路线 不用画
                            continue;
                        }
                        int key2 = routeOverlays.keyAt(j);
                        routeOverlays.get(key2).setTransparency(0.4f);
                        routeOverlays.get(key2).setZindex(0);
                    }
                    routeOverlays.get(key).setTransparency(1.0f);
                    /**把用户选择的那条路的权值弄高，使路线高亮显示的同时，重合路段不会变的透明**/
                    routeOverlays.get(key).setZindex(1);
                    mAMapNavi.selectRouteId(key);
                    routeIndex = i;
                    chooseRouteSuccess = true;
                    return;
                }
            }
        }
    }
}
