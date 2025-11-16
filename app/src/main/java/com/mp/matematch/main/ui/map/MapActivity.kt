package com.mp.matematch.main.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*

import com.mp.matematch.databinding.ActivityMapBinding
import com.mp.matematch.R

// Location Services Import
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority

// Firebase 관련 Import
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.core.graphics.toColorInt

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth

/**
 * 💡 Constants
 * 라벨 및 레이어 관리를 위한 고유 ID를 정의합니다.
 */
private const val MY_LOCATION_STYLE_ID = "my_location_style"
private const val OTHER_USER_STYLE_ID = "other_user_style"
private const val OTHER_USER_LAYER_ID = "other_users_layer"
private const val LOCATION_COLLECTION_NAME = "userLocations" // Firestore 컬렉션 이름

/**
 * 💡 DummyR (실제 프로젝트의 R.color 리소스를 대체함)
 * DummyR 객체는 사용하지 않도록 `toColorInt()`를 사용하는 방식으로 변경하거나 제거를 권장합니다.
 * 현재 코드에서는 Kakao Map의 LabelStyle 정의에서 DummyR을 사용하지 않아 문제가 없습니다.
 */

// 💡 Firestore 데이터 모델 (타 사용자의 위치만 저장)
data class OtherUserLocation(
    val userId: String = "",         // 문서 ID 또는 사용자 UID
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0) // 위치 정보
) {
    // 지도 라벨 표시를 위한 편의 getter
    val latitude: Double get() = geoPoint.latitude
    val longitude: Double get() = geoPoint.longitude
}

class MapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private lateinit var mapView: MapView // ✅ lateinit 변수 유지
    private var kakaoMap: KakaoMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var locationListener: ListenerRegistration? = null

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    // ✅ 등록된 LabelStyles 객체를 저장하는 변수
    private var myRegisteredStyles: LabelStyles? = null
    private var otherUserRegisteredStyles: LabelStyles? = null

    // ✅ LabelLayer 객체를 저장하는 변수 (레이어 분리 관리)
    private var myLabelLayer: LabelLayer? = null
    private var otherUserLabelLayer: LabelLayer? = null

    // ✅ Firebase Auth에서 가져온 실제 사용자 ID를 저장하는 변수
    private var currentUserId: String = "GUEST_INIT"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Firebase 초기화
        FirebaseApp.initializeApp(this)
        db = Firebase.firestore
        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this) // ✅ FusedLocationClient 초기화

        currentUserId = auth.currentUser?.uid ?: run {
            Log.w("MapActivity", "⚠️ Firebase currentUser가 null입니다. 임시 Guest ID를 사용합니다.")
            "GUEST_${System.currentTimeMillis()}"
        }
        Log.d("MapActivity", "✅ 현재 MapActivity의 사용자 ID: $currentUserId")

        // 2. 위치 요청 객체 설정 (10초마다 위치 업데이트)
        locationRequest = LocationRequest.create().apply {
            interval = 10000 // 10초
            fastestInterval = 5000 // 5초
            priority = Priority.PRIORITY_HIGH_ACCURACY // PRIORITY_HIGH_ACCURACY로 수정
        }

        // 3. 위치 업데이트 콜백 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let { location ->
                    processLocationUpdate(location)
                }
            }
        }

        // 🚨 CRASH FIX: MapView 변수 초기화
        mapView = binding.mapView // ✅ [필수 수정] lateinit mapView 초기화

        // 4. 줌 인/아웃 버튼 연결 및 GPS 버튼 연결
        try {
            binding.btnZoomOut.setOnClickListener { zoomOutMap() }
            binding.btnZoomIn.setOnClickListener { zoomInMap() }
            binding.btnMyLocation.setOnClickListener { moveCameraToMyLocation() }
        } catch (e: Exception) {
            Log.w("MapActivity", "버튼 연결 실패. activity_map.xml 레이아웃 ID 확인 필요: ${e.message}")
        }

        // 5. Kakao Map 초기화 시작
        mapView.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("MapActivity", "지도 정상 종료됨")
            }

            override fun onMapError(error: Exception) {
                Log.e("MapActivity", "지도 에러: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                Log.d("MapActivity", "✅ 지도 준비 완료")
                kakaoMap = map

                // 1. LabelStyles 정의, 등록, 및 저장
                setupLabelStyles(map.labelManager!!)

                // 2. 기본 카메라 위치 (서울 시청 근처, 대한민국 중앙)로 변경
                val startPosition = LatLng.from(37.5665, 126.9780)
                val cameraUpdate = CameraUpdateFactory.newCenterPosition(startPosition, 9)
                kakaoMap?.moveCamera(cameraUpdate)

                // 3. 💡 위치 권한 요청 및 위치 업데이트 시작
                requestLocationPermissionAndStartUpdate()

                // 4. ✅ Firestore 리스너 시작하여 타 사용자 위치 실시간 구독
                startLocationListener()
            }

            override fun getMapViewInfo(): MapViewInfo {
                return MapViewInfo.from("openmap", MapType.NORMAL)
            }
        })
    }

    private fun zoomOutMap() {
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.zoomOut())
            Log.d("MapActivity", "🗺️ 줌 아웃 실행됨.")
        } ?: run {
            Log.w("MapActivity", "KakaoMap 객체가 아직 준비되지 않아 줌 아웃할 수 없습니다.")
        }
    }

    private fun zoomInMap() {
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.zoomIn())
            Log.d("MapActivity", "🗺️ 줌 인 실행됨.")
        } ?: run {
            Log.w("MapActivity", "KakaoMap 객체가 아직 준비되지 않아 줌 인할 수 없습니다.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("MapActivity", "내 위치로 이동: 위치 권한이 없습니다.")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val myLatLng = LatLng.from(location.latitude, location.longitude)
                val targetZoom = kakaoMap?.cameraPosition?.zoomLevel ?: 15
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myLatLng, targetZoom))
                Log.d("MapActivity", "📍 GPS 버튼 클릭: 현재 위치로 카메라 이동.")
            } else {
                Log.w("MapActivity", "현재 위치를 가져올 수 없습니다. 위치 권한 및 GPS 활성화 상태를 확인하세요.")
            }
        }
    }

    private fun requestLocationPermissionAndStartUpdate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            Log.d("GPS", "🚀 위치 업데이트 시작됨.")
        } else {
            Log.e("GPS", "❌ 위치 권한이 없어 업데이트를 시작할 수 없습니다.")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("GPS", "🛑 위치 업데이트 중지됨.")
    }

    private fun startLocationListener() {
        Log.d("Firestore", "✅ 타 사용자 위치 리스너 등록 완료")

        locationListener = db.collection(LOCATION_COLLECTION_NAME)
            .whereNotEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", "❌ 위치 리스너 실패: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val otherUsers = snapshots.documents.mapNotNull { document ->
                        // 타 사용자 위치 모델을 축약된 버전으로 파싱
                        document.toObject(OtherUserLocation::class.java)?.copy(userId = document.id)
                    }.filter {
                        it.geoPoint.latitude != 0.0 || it.geoPoint.longitude != 0.0
                    }

                    showOtherUsersLocations(otherUsers)
                }
            }
    }

    /**
     * LabelStyles를 정의하고 LabelManager에 등록 후, 그 객체를 저장합니다.
     */
    private fun setupLabelStyles(labelManager: LabelManager) {

        // 1. 내 위치 스타일 정의: 아이콘 사용
        // R.drawable.ic_menu_add 리소스 파일이 존재하는지 확인해주세요.
        val myLocationStyle = LabelStyle.from(R.drawable.ic_menu_add)
            .setTextStyles(
                // 스타일 1: 크기 32, 색상 #DB5461
                LabelTextStyle.from(32, "#DB5461".toColorInt()))

        // 2. 타 사용자 스타일 정의: 아이콘 사용
        // ✅ [수정] 텍스트 스타일을 명시적으로 추가합니다.
        // - 아이콘: R.drawable.ic_logo (유효함이 확인됨)
        // - 텍스트: 크기 24, 색상 #4A90E2 (파란색 계열)
        val otherUserStyle = LabelStyle.from(R.drawable.ic_smalllogo)



        // 3. LabelStyles 객체 생성 및 등록 (반환 값을 변수에 저장)
        myRegisteredStyles = labelManager.addLabelStyles(
            LabelStyles.from(MY_LOCATION_STYLE_ID, myLocationStyle)
        )

        otherUserRegisteredStyles = labelManager.addLabelStyles(
            LabelStyles.from(OTHER_USER_STYLE_ID, otherUserStyle)
        )



        // 4. 레이어 초기화
        // onMapReady 내부에서 호출하므로 kakaoMap은 non-null이 보장됩니다.
        myLabelLayer = kakaoMap!!.labelManager!!.getLayer()
        otherUserLabelLayer = labelManager.addLayer(
            LabelLayerOptions.from(OTHER_USER_LAYER_ID)
        )
        Log.d("MapActivity", "✅ LabelStyles 및 LabelLayers 설정 완료 (최소 버전)")
    }


    private fun processLocationUpdate(location: Location) {
        if (kakaoMap == null) {
            Log.w("GPS", "지도 객체(kakaoMap)가 준비되지 않아 위치 업데이트를 건너킵니다.")
            return
        }

        val myLatLng = LatLng.from(location.latitude, location.longitude)

        updateMyPositionToFirestore(location)

        // 카메라 이동 (내 위치를 따라가게 함)
        // 줌 레벨을 유지하며 중앙 위치만 업데이트하도록 수정
        val targetZoom = kakaoMap?.cameraPosition?.zoomLevel ?: 15
        kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myLatLng, targetZoom))

        // 3. 내 위치 라벨 표시
        val styles = myRegisteredStyles
        if (styles == null) {
            Log.e("MapActivity", "❌ 내 위치 스타일이 등록되지 않았습니다.")
            return
        }

        // 라벨 텍스트를 "나" 또는 "My Location"으로 단순화
        val labelOptions = LabelOptions.from(myLatLng)
            .setTag(0)
            .setStyles(styles)
            .setTexts(LabelTextBuilder().setTexts("My Location"))



        myLabelLayer?.removeAll()
        myLabelLayer?.addLabel(labelOptions)

        Log.d("GPS", "📍 내 위치: ${location.latitude}, ${location.longitude} (업데이트 및 라벨 표시 완료)")
    }

    private fun updateMyPositionToFirestore(location: Location) {
        // ✅ 사용자 ID가 초기화되지 않은 경우 데이터 저장 방지
        if (currentUserId == "GUEST_INIT" || currentUserId.startsWith("GUEST_")) {
            Log.w("Firestore", "⚠️ Firebase Auth UID가 없습니다. 위치 정보 저장하지 않습니다.")
            return
        }

        // ✅ 타 사용자에게 필요 없는 'name' 필드 제거, 'userId'와 'geoPoint'만 저장하도록 축약
        val userLocationData = mapOf(
            "userId" to currentUserId,
            "geoPoint" to GeoPoint(location.latitude, location.longitude),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection(LOCATION_COLLECTION_NAME)
            .document(currentUserId)
            .set(userLocationData)
            .addOnSuccessListener {
                Log.d("Firestore", "✅ 내 위치 (${currentUserId}) Firestore 업데이트 성공")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ 내 위치 Firestore 업데이트 실패", e)
            }
    }

    fun showOtherUsersLocations(otherUsers: List<OtherUserLocation>) {
        val layer = otherUserLabelLayer ?: return

        val styles = otherUserRegisteredStyles
        if (styles == null) {
            Log.e("MapActivity", "❌ 타 사용자 스타일이 등록되지 않았습니다. (setupLabelStyles 확인 필요)")
            return
        }

        layer.removeAll()

        val newLabels = otherUsers.map { user ->
            // ✅ 라벨 텍스트를 user ID의 처음 4자리로만 표시하도록 축약
            val displayId = user.userId.take(4)
            LabelOptions.from(LatLng.from(user.latitude, user.longitude))
                .setTag(user.userId.hashCode())
                .setStyles(styles)
                .setTexts(LabelTextBuilder().setTexts(displayId))
        }

        if (newLabels.isNotEmpty()) {
            layer.addLabels(newLabels)
            Log.d("MapActivity", "👥 타 사용자 ${newLabels.size}명 라벨 표시 완료")
        } else {
            Log.d("MapActivity", "👥 타 사용자 라벨 없음 (0명)") // ✅ 로그 추가: 빈 리스트일 때도 기록
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Log.e("Permission", "위치 권한 거부됨")
            }
        }

    override fun onResume() {
        super.onResume()
        mapView.resume()
        requestLocationPermissionAndStartUpdate()
    }

    override fun onPause() {
        super.onPause()
        mapView.pause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.finish() // ✅ Kakao Map 리소스 해제는 finish()가 더 적절합니다.
        locationListener?.remove()
    }
}