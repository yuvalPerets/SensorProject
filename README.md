# Autism Meltdown Detection System

A real-time wearable detection system that helps caregivers identify early signs of meltdowns in individuals on the autism spectrum using machine learning and sensor data analysis.

## 🎯 Project Overview

This system was developed as a final project for Computer Engineering degree, achieving **90%+ prediction accuracy** in real-time meltdown detection. The solution combines wearable technology, mobile applications, and artificial neural networks to provide caregivers with timely alerts when individuals with autism exhibit movement patterns associated with meltdowns.

## ✨ Key Features

- **Real-time Detection**: Continuous monitoring and analysis of movement patterns
- **High Accuracy**: Achieved 90%+ classification accuracy using ANN
- **Wearable Integration**: Samsung Galaxy Watch app for seamless data collection
- **Caregiver Dashboard**: Mobile app interface for monitoring and alerts
- **Adaptive Learning**: System improves over time with new patient data
- **Customizable Alerts**: Configurable thresholds for different movement indicators
- **Cloud Integration**: Firebase backend for data storage and model training

## 🏗️ System Architecture

### Components:
1. **Galaxy Watch App** - Sensor data collection and Bluetooth transmission
2. **Android Mobile App** - Data processing, ML inference, and caregiver interface
3. **Web Service** - Neural network training and model management
4. **Firebase Backend** - Data storage and real-time synchronization

### Data Flow:
```
Galaxy Watch → Bluetooth → Mobile App → Web Service → ANN Model → Predictions → Caregiver Interface
```

## 🔧 Technical Implementation

### Galaxy Watch Application
- **Language**: Android/Wear OS
- **Sensors**: Accelerometer data collection
- **Communication**: Bluetooth Low Energy (BLE) for data transmission
- **Service**: Background service for continuous monitoring

### Mobile Application (Android)
- **Data Processing**: Real-time signal processing in time and frequency domains
- **Machine Learning**: On-device inference for immediate results
- **User Interface**: Caregiver dashboard with alert system
- **Database**: Firebase integration for data collection and storage
- **Features**:
  - Real-time parameter computation
  - Custom threshold settings
  - Movement classification interface
  - Model retraining capabilities

### Web Service & Neural Network
- **Framework**: Python-based ANN implementation
- **Training**: Automated model training with new data
- **Classification**: Multi-class movement pattern recognition
- **API**: RESTful endpoints for model inference and training

### Database & Backend
- **Firebase**: Real-time database for data synchronization
- **Data Collection**: Continuous learning from user feedback
- **Model Storage**: Version control for trained models

## 📊 Performance Metrics

- **Classification Accuracy**: 90%+ prediction rate
- **Real-time Processing**: Sub-second response time
- **System Reliability**: Continuous operation without interruption
- **Adaptability**: Model improves with additional training data

## 🚀 Key Achievements

- Developed end-to-end IoT solution combining wearable, mobile, and cloud technologies
- Implemented real-time signal processing for time and frequency domain analysis
- Created adaptive machine learning system that improves with user feedback
- Achieved high accuracy in movement classification for autism meltdown detection
- Built scalable architecture supporting multiple patients and caregivers

## 💡 Innovation Highlights

- **Personalized Detection**: System adapts to individual patient movement patterns
- **Caregiver Empowerment**: Allows caregivers to define custom indicators and thresholds
- **Continuous Learning**: Model retraining capabilities ensure improved accuracy over time
- **Real-time Analysis**: Immediate processing and alert generation

## 🛠️ Technologies Used

**Hardware & Sensors:**
- Samsung Galaxy Watch
- Accelerometer sensors
- Bluetooth connectivity

**Mobile Development:**
- Android SDK
- Wear OS SDK
- Bluetooth Low Energy APIs

**Backend & ML:**
- Python
- Artificial Neural Networks
- Firebase
- RESTful APIs

**Signal Processing:**
- Time domain analysis
- Frequency domain analysis
- Real-time parameter computation

## 📱 User Interface

The mobile application provides caregivers with:
- Real-time movement status display
- Customizable alert thresholds
- Movement pattern classification interface
- Historical data visualization
- Model training controls

## 🎯 Impact & Applications

This system addresses a critical need in autism care by providing:
- Early intervention opportunities
- Reduced caregiver stress and anxiety
- Improved quality of life for individuals with autism
- Data-driven insights into behavioral patterns
- Scalable solution for care facilities and families

## 🔮 Future Enhancements

- Integration with additional sensors (heart rate, skin conductance)
- Advanced ML models (deep learning, ensemble methods)
- Multi-platform support (iOS, other wearables)
- Caregiver mobile app for remote monitoring
- Integration with healthcare systems

## 👨‍💻 About the Developer

This project demonstrates expertise in:
- Full-stack development (mobile, web, embedded)
- Machine learning and signal processing
- IoT system design and implementation
- Healthcare technology solutions
- Real-time system development

---

*Developed as a Computer Engineering final project, showcasing the integration of wearable technology, machine learning, and healthcare applications to create meaningful solutions for the autism community.*
