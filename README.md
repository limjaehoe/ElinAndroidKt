# ElinAndroidKt - X-ray 장비 제어 안드로이드 앱

## 프로젝트 개요
이 앱은 의료용 X-ray 장비를 제어하는 안드로이드 애플리케이션입니다. CAN 통신으로 장비 하드웨어와 통신하고, TCP/IP 통신을 통해 Viewer 시스템과 데이터를 교환합니다.

## 주요 기능
- X-ray 장비(Ceiling, Table, Stand, Collimator) 제어
- 환자 정보 및 촬영 정보 관리
- 의료 영상 획득 및 전송
- 암호화된 통신 프로토콜

## 기술 스택
- Kotlin
- Android Jetpack (ViewModel, LiveData)
- Hilt (의존성 주입)
- CAN 통신 (USB to CAN)
- TCP/IP 통신

## 통신 프로토콜
자세한 통신 프로토콜은 다음 문서를 참조하세요:
- [CAN 통신 프로토콜](docs/CAN_Protocol.md)
- [Viewer 통신 프로토콜](docs/Viewer_Communication.md)
