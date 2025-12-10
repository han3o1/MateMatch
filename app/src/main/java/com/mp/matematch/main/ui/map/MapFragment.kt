package com.mp.matematch.main.ui.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.kakao.vectormap.*
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.*

import com.mp.matematch.databinding.FragmentMapBinding
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
 *  Constants (상수들은 그대로 유지)
 */
private const val MY_LOCATION_STYLE_ID = "my_location_style"
private const val OTHER_USER_STYLE_ID = "other_user_style"
private const val OTHER_USER_LAYER_ID = "other_users_layer"
private const val LOCATION_COLLECTION_NAME = "userLocations"

// userId → Label 객체 저장
private val otherUserMarkers = mutableMapOf<String, Label>()

//  Firestore 데이터 모델 (이동 없음)
data class OtherUserLocation(
    val userId: String = "",
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
) {
    val latitude: Double get() = geoPoint.latitude
    val longitude: Double get() = geoPoint.longitude
}

class MapFragment : Fragment() {

    // 뷰 바인딩은 Fragment의 생명주기에 맞춰 _binding 변수를 사용하고,
    // nullable 타입으로 선언 후 onDestroyView에서 null로 설정합니다.

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!! // 뷰에 접근할 때 null 체크 없이 사용하기 위한 getter

    private var mapView: MapView? = null // MapView는 Fragment 생명주기에 맞춰 null 허용으로 변경
    private var kakaoMap: KakaoMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var locationListener: ListenerRegistration? = null

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest

    private var myRegisteredStyles: LabelStyles? = null
    private var otherUserRegisteredStyles: LabelStyles? = null

    private var myLabelLayer: LabelLayer? = null
    private var otherUserLabelLayer: LabelLayer? = null

    private var currentUserId: String = "GUEST_INIT"

    // --- Fragment 생명주기 시작 ---

    /**
     * 뷰 바인딩 초기화 및 레이아웃 인플레이션
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }


    /**
     * 뷰 초기화 및 객체 초기화 (onCreate()의 역할 일부 대체)
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Firebase 및 Location Client 초기화
        val context = requireContext()
        FirebaseApp.initializeApp(context)
        db = Firebase.firestore
        auth = Firebase.auth
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

        currentUserId = auth.currentUser?.uid ?: run {
            Log.w("MapFragment", " Firebase currentUser가 null입니다. 임시 Guest ID를 사용합니다.")
            "GUEST_${System.currentTimeMillis()}"
        }
        Log.d("MapFragment", " 현재 MapFragment의 사용자 ID: $currentUserId")

        // 2. 위치 요청 객체 설정 (10초마다 위치 업데이트)
        locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = Priority.PRIORITY_HIGH_ACCURACY
        }

        // 3. 위치 업데이트 콜백 정의
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.locations.lastOrNull()?.let { location ->
                    processLocationUpdate(location)
                }
            }
        }

        // 4. MapView 초기화 및 버튼 연결
        mapView = binding.mapView
        setupButtons()

        // 5. Kakao Map 초기화 시작
        mapView?.start(object : MapLifeCycleCallback() {
            override fun onMapDestroy() {
                Log.d("MapFragment", "지도 정상 종료됨")
            }

            override fun onMapError(error: Exception) {
                Log.e("MapFragment", "지도 에러: ${error.message}")
            }
        }, object : KakaoMapReadyCallback() {
            override fun onMapReady(map: KakaoMap) {
                Log.d("MapFragment", " 지도 준비 완료")
                kakaoMap = map

                // 1. LabelStyles 정의, 등록, 및 저장
                setupLabelStyles(map.labelManager!!)

                // 2. 기본 카메라 위치로 변경
                val startPosition = LatLng.from(37.5665, 126.9780)
                val cameraUpdate = CameraUpdateFactory.newCenterPosition(startPosition, 9)
                kakaoMap?.moveCamera(cameraUpdate)

                // 3. 위치 권한 요청 및 위치 업데이트 시작
                requestLocationPermissionAndStartUpdate()

                // 4. Firestore 리스너 시작
                startLocationListener()
            }

            override fun getMapViewInfo(): MapViewInfo {
                return MapViewInfo.from("openmap", MapType.NORMAL)
            }
        })
    }

    /**
     * 버튼 클릭 리스너 연결
     */
    private fun setupButtons() {
        try {
            binding.btnZoomOut.setOnClickListener { zoomOutMap() }
            binding.btnZoomIn.setOnClickListener { zoomInMap() }
            binding.btnMyLocation.setOnClickListener {
                Log.d("TEST", "내 위치 버튼 눌림!!")
                moveCameraToMyLocation() }
        } catch (e: Exception) {
            Log.w("MapFragment", "버튼 연결 실패. fragment_map.xml 레이아웃 ID 확인 필요: ${e.message}")
        }
    }

    /**
     * MapView 생명주기 연결 (onResume, onPause, onDestroy)
     */
    override fun onResume() {
        super.onResume()
        mapView?.resume() // MapView의 resume 호출
        // Fragment가 다시 활성화될 때 위치 업데이트 다시 시작
        // startLocationUpdates() 대신 권한 확인 로직을 다시 태움
        if (kakaoMap != null) {
            requestLocationPermissionAndStartUpdate()
        }
    }

    override fun onPause() {
        super.onPause()
        mapView?.pause() // MapView의 pause 호출
        stopLocationUpdates() // 위치 업데이트 중지
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //mapView?.finish() // MapView 리소스 해제
        locationListener?.remove() // Firestore 리스너 해제
        kakaoMap = null
        mapView = null
        _binding = null // 뷰 바인딩 해제
    }

    // --- 기존 Activity 메서드 변환 ---

    private fun zoomOutMap() {
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.zoomOut())
            Log.d("MapFragment", "줌 아웃 실행됨.")
        } ?: run {
            Log.w("MapFragment", "KakaoMap 객체가 아직 준비되지 않아 줌 아웃할 수 없습니다.")
        }
    }

    private fun zoomInMap() {
        kakaoMap?.let { map ->
            map.moveCamera(CameraUpdateFactory.zoomIn())
            Log.d("MapFragment", "줌 인 실행됨.")
        } ?: run {
            Log.w("MapFragment", "KakaoMap 객체가 아직 준비되지 않아 줌 인할 수 없습니다.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToMyLocation() {
        val context = context ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.w("MapFragment", "내 위치로 이동: 위치 권한이 없습니다.")
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                val myLatLng = LatLng.from(location.latitude, location.longitude)
                val targetZoom = kakaoMap?.cameraPosition?.zoomLevel ?: 15
                kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myLatLng, targetZoom))
                Log.d("MapFragment", " GPS 버튼 클릭: 현재 위치로 카메라 이동.")
            } else {
                Log.w("MapFragment", "현재 위치를 가져올 수 없습니다. 위치 권한 및 GPS 활성화 상태를 확인하세요.")
            }
        }
    }

    private fun requestLocationPermissionAndStartUpdate() {
        val context = context ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val context = context ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            Log.d("GPS", " 위치 업데이트 시작됨.")
        } else {
            Log.e("GPS", " 위치 권한이 없어 업데이트를 시작할 수 없습니다.")
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d("GPS", "위치 업데이트 중지됨.")
    }

    private fun startLocationListener() {
        Log.d("Firestore", " 타 사용자 위치 리스너 등록 완료")

        locationListener = db.collection(LOCATION_COLLECTION_NAME)
            .whereNotEqualTo("userId", currentUserId)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("Firestore", " 위치 리스너 실패: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val otherUsers = snapshots.documents.mapNotNull { document ->
                        document.toObject(OtherUserLocation::class.java)?.copy(userId = document.id)
                    }.filter {
                        it.geoPoint.latitude != 0.0 || it.geoPoint.longitude != 0.0
                    }

                    showOtherUsersLocations(otherUsers)
                }
            }
    }

    private fun setupLabelStyles(labelManager: LabelManager) {
        // 1. 내 위치 스타일 정의
        val myLocationStyle = LabelStyle.from(R.drawable.ic_smalllogo)



        // 2. 타 사용자 스타일 정의
        val otherUserStyle = LabelStyle.from(R.drawable.ic_smalllogo)

        // 3. LabelStyles 객체 생성 및 등록
        myRegisteredStyles = labelManager.addLabelStyles(
            LabelStyles.from(MY_LOCATION_STYLE_ID, myLocationStyle)
        )

        otherUserRegisteredStyles = labelManager.addLabelStyles(
            LabelStyles.from(OTHER_USER_STYLE_ID, otherUserStyle)
        )

        // 4. 레이어 초기화
        myLabelLayer = kakaoMap?.labelManager?.getLayer()
        otherUserLabelLayer = labelManager.addLayer(
            LabelLayerOptions.from(OTHER_USER_LAYER_ID)
        )
        Log.d("MapFragment", " LabelStyles 및 LabelLayers 설정 완료")
    }


    private fun processLocationUpdate(location: Location) {
        if (kakaoMap == null) {
            Log.w("GPS", "지도 객체(kakaoMap)가 준비되지 않아 위치 업데이트를 건너킵니다.")
            return
        }

        val myLatLng = LatLng.from(location.latitude, location.longitude)

        updateMyPositionToFirestore(location)

        // 카메라 이동 (내 위치를 따라가게 함)
        val targetZoom = kakaoMap?.cameraPosition?.zoomLevel ?: 15
        //kakaoMap?.moveCamera(CameraUpdateFactory.newCenterPosition(myLatLng, targetZoom))

        // 3. 내 위치 라벨 표시
        val styles = myRegisteredStyles
        if (styles == null || myLabelLayer == null) {
            Log.e("MapFragment", " 내 위치 스타일 또는 레이어가 등록되지 않았습니다.")
            return
        }

        val labelOptions = LabelOptions.from(myLatLng)
            .setTag(0)
            .setStyles(styles)


        myLabelLayer?.removeAll()
        myLabelLayer?.addLabel(labelOptions)

        Log.d("GPS", " 내 위치: ${location.latitude}, ${location.longitude} (업데이트 및 라벨 표시 완료)")
    }

    private fun updateMyPositionToFirestore(location: Location) {
        if (currentUserId == "GUEST_INIT" || currentUserId.startsWith("GUEST_")) {
            Log.w("Firestore", "️ Firebase Auth UID가 없습니다. 위치 정보 저장하지 않습니다.")
            return
        }

        val userLocationData = mapOf(
            "userId" to currentUserId,
            "geoPoint" to GeoPoint(location.latitude, location.longitude),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection(LOCATION_COLLECTION_NAME)
            .document(currentUserId)
            .set(userLocationData)
            .addOnSuccessListener {
                Log.d("Firestore", "내 위치 (${currentUserId}) Firestore 업데이트 성공")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", " 내 위치 Firestore 업데이트 실패", e)
            }
    }

    fun showOtherUsersLocations(otherUsers: List<OtherUserLocation>) {
        val layer = otherUserLabelLayer ?: return
        val styles = otherUserRegisteredStyles ?: return

        val updatedUserIds = otherUsers.map { it.userId }.toSet()

        //  1) 사라진 유저 라벨 정리
        val iterator = otherUserMarkers.iterator()
        while (iterator.hasNext()) {
            val (userId, label) = iterator.next()
            if (userId !in updatedUserIds) {
                layer.remove(label)   //  removeLabel → remove 로 수정
                iterator.remove()
            }
        }

        //  2) 새/기존 유저 업데이트
        otherUsers.forEach { user ->
            val pos = LatLng.from(user.latitude, user.longitude)

            if (otherUserMarkers.containsKey(user.userId)) {
                // 기존 라벨은 remove 후 다시 add (move 기능 없음)
                val oldLabel = otherUserMarkers[user.userId]
                layer.remove(oldLabel)

                val newLabel = layer.addLabel(
                    LabelOptions.from(pos)
                        .setStyles(styles)
                        .setTag(user.userId.hashCode())
                        .setTexts(LabelTextBuilder().setTexts(user.userId.take(4)))
                )
                otherUserMarkers[user.userId] = newLabel
                return@forEach
            }

            //  3) 새로운 유저 라벨 추가
            val newLabel = layer.addLabel(
                LabelOptions.from(pos)
                    .setStyles(styles)
                    .setTag(user.userId.hashCode())
                    .setTexts(LabelTextBuilder().setTexts(user.userId.take(4)))
            )

            if (newLabel != null) {
                otherUserMarkers[user.userId] = newLabel
            }
        }

        Log.d("MapFragment", " 라벨 업데이트 완료 (최적화 적용): ${otherUserMarkers.size}명")
    }


//    fun showOtherUsersLocations(otherUsers: List<OtherUserLocation>) {
//        val layer = otherUserLabelLayer ?: return
//
//        val styles = otherUserRegisteredStyles
//        if (styles == null) {
//            Log.e("MapFragment", "타 사용자 스타일이 등록되지 않았습니다.")
//            return
//        }
//
//        layer.removeAll()
//
//        val newLabels = otherUsers.map { user ->
//            val displayId = user.userId.take(4)
//            LabelOptions.from(LatLng.from(user.latitude, user.longitude))
//                .setTag(user.userId.hashCode())
//                .setStyles(styles)
//                .setTexts(LabelTextBuilder().setTexts(displayId))
//        }
//
//        if (newLabels.isNotEmpty()) {
//            layer.addLabels(newLabels)
//            Log.d("MapFragment", "👥 타 사용자 ${newLabels.size}명 라벨 표시 완료")
//        } else {
//            Log.d("MapFragment", "👥 타 사용자 라벨 없음 (0명)")
//        }
//    }

    //  Fragment의 권한 요청 런처
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startLocationUpdates()
            } else {
                Log.e("Permission", "위치 권한 거부됨")
                // 사용자에게 권한이 필요함을 알리는 UI 업데이트 또는 토스트 메시지를 여기에 추가할 수 있습니다.
            }
        }
}