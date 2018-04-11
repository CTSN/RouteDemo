最近项目上有用到高德sdk，需求上要求多路线规划，并且可以在地图上点击选择路线。本着不想重复造轮子的驱使下(其实想偷懒)，网上似乎没有例子,也可能是我找不到。最后没办法，只能自己实现。
在此做为记录，如果有更好方法，劳烦指出。

实现效果如下：

![这里写图片描述](https://img-blog.csdn.net/20180411180555737?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

### 实现思路
当点击地图线段的时候获取对应坐标点，然后跟路线上的坐标点比对，如果经纬度各自的差值都小于0.000001，则选择该路线。
PS:为啥小于0.00001，是考虑到两条路线如果坐标相差很近时，给的一个差值范围
### 实现思路步骤
#### 1.获取屏幕点击线段的坐标
在多路线规划完成后(多路线规划的在这就不叙述了，官网例子较详细)，可以设置AMap的setOnPolylineClickListener方法监听。

代码如下:

```Java
 //.....多余代码省略
 {
	 mAmap.setOnPolylineClickListener(this);
	 //......多余代码省略
 }
 @Override
 public void onPolylineClick(Polyline polyline) {
 }
```

该方法是监听地图上线段点击回调。我们路线规划出来，在屏幕上就是一段一段线段拼接出来的。
回调出来这个类Polyline 究竟是什么呢？
查看高德提供的参考手册

![这里写图片描述](https://img-blog.csdn.net/20180411162127562?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

回调出来就是点击的该线段，我们继续看参考手册，看提供的相应方法。

![这里写图片描述](https://img-blog.csdn.net/20180411162344365?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)

里面有个获取线段顶点坐标列表的方法**getPoints()**。
我们先把这个经纬度坐标列表打印出来看下。

<img src="https://img-blog.csdn.net/20180411163211624?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70" width="500px">
在此我们第一步骤就完成了。

#### 获取路线对应的坐标列表
高德地图中路线这个类对应的是**RouteOverLay**这个类。

![这里写图片描述](https://img-blog.csdn.net/2018041116391926?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
同样的我们得去看下高德提供参考手册，有哪些方法可以提供。在翻看很久情况下都没找到类似**LatLng**经纬度数据这个类，但是其中有个方法是获取导航路径对象——**getAMapNaviPath()**。

![这里写图片描述](https://img-blog.csdn.net/20180411164249943?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
我们再点击查看这个类有什么方法是可以获取经纬度列表的。果然有获取坐标的列表。

![这里写图片描述](https://img-blog.csdn.net/20180411164435393?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70)
同样的我们把这个经纬度集合打印出来看下，是不是预想效果。

<img src="https://img-blog.csdn.net/20180411164826591?watermark/2/text/aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L1ZSb3ltb25k/font/5a6L5L2T/fontsize/400/fill/I0JBQkFCMA==/dissolve/70" width="500px">
打印出来的经纬度坐标巨多，因为路线规划是多个线段的组合所以坐标会很多。
#### 坐标比较，选择对应路线
好了现在地图线段坐标集合和路线对应的坐标集合都有了，如何做比较呢？
我的思路是这样的，取线段坐标集合任意一点跟路线对应的坐标集做差值计算，如果经纬度都小于0.000001我们一开始设定的范围，则认为点击到该路线，则把该路线高亮显示。
代码如下：

```Android
@Override
    public void onPolylineClick(Polyline polyline) {
	    if（ routeOverlays != null && routeOverlays.size() == 1）{  //路线只有一条，没必要选择路线。
		    return;
	    }
        List<LatLng> latLngs = polyline.getPoints();
        if (latLngs.size() == 0){//确定获取线段有坐标集合
            return;
        }
        LatLng latLng = latLngs.get(0);//取线段的第一个坐标就好
        outer:
        for (int i = 0; i < routeOverlays.size(); i++) {//遍历路线集合
            int key = routeOverlays.keyAt(i);
            List<NaviLatLng> naviLatLngs = routeOverlays.get(key)
					            .getAMapNaviPath().getCoordList();  //获取路线所有坐标集合
            for (NaviLatLng naviLatLng : naviLatLngs) {//遍历路线的坐标集合
                if (Math.abs((naviLatLng.getLatitude() - latLng.latitude)) <= 0.000001
                        && Math.abs((naviLatLng.getLongitude()- latLng.longitude)) <= 0.00001){//符合差值范围
                    if (i == routeIndex){   // 已经选过该路线 跳转下一条路线 点击的线段可能是两条路线的重复路段
                        continue outer;
                    }
                    //下面循环方法是绘制没选中路线不高亮
                    for (int j = 0; j < routeOverlays.size(); j++) {
                        if (i == j){    //选中路线 先不用画
                            continue;
                        }
                        int key2 = routeOverlays.keyAt(j);
                        routeOverlays.get(key2).setTransparency(0.4f);
                        routeOverlays.get(key).setZindex(0);
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
```

至此，整体的思路就是这样，具体Demo呢，还是找个周末放到Github上去。
实现方法如有不足之处，请指出。
题外话：看自己上篇博客的时间已经是去年了，自己偷懒了，得多学习了。
