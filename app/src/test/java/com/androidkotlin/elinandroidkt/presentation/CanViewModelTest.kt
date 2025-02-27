package com.androidkotlin.elinandroidkt.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.androidkotlin.elinandroidkt.data.CanData
import com.androidkotlin.elinandroidkt.data.CommunicationResult
import com.androidkotlin.elinandroidkt.domain.CanCommunication
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@ExperimentalCoroutinesApi
class CanViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
    private lateinit var canCommunication: CanCommunication
    private lateinit var viewModel: CanViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        canCommunication = mock()
        viewModel = CanViewModel(canCommunication)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when startCanCommunication is called then connect is called`() = runTest {
        // Given
        whenever(canCommunication.connect()).thenReturn(CommunicationResult.Success(Unit))
        whenever(canCommunication.receiveData()).thenReturn(flowOf())

        // When
        viewModel.startCanCommunication()

        // Then
        verify(canCommunication).connect()
    }

    @Test
    fun `when data is received, canData should be updated`() = runTest {
        // Given
        val testCanData = CanData(
            id = 0x040,
            cmd = 0x00,
            dlc = 6,
            data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        )

        // 데이터를 방출할 MutableSharedFlow 생성
        val testFlow = MutableSharedFlow<CommunicationResult<CanData>>()

        // canCommunication의 동작 설정
        whenever(canCommunication.connect()).thenReturn(CommunicationResult.Success(Unit))
        whenever(canCommunication.receiveData()).thenReturn(testFlow)

        // ViewModel의 통신 시작
        viewModel.startCanCommunication()

        // 데이터 방출 전에 초기값 확인
        assertEquals(null, viewModel.canData.value)

        // When - 테스트 데이터 방출
        testFlow.emit(CommunicationResult.Success(testCanData))

        // Then - viewModel의 canData 상태가 업데이트 되었는지 확인
        assertEquals(testCanData, viewModel.canData.value)
    }

    @Test
    fun `when error occurs during connection, state should be updated with error`() = runTest {
        // Given
        val exception = Exception("Connection error")
        whenever(canCommunication.connect()).thenReturn(CommunicationResult.Error(exception))

        // When
        viewModel.startCanCommunication()

        // Then
        assertEquals(CommunicationResult.Error(exception), viewModel.connectionState.value)
    }

    @Test
    fun testCanSend() = runTest {
        // Given
        val testCanData = CanData(
            id = 0x040,
            cmd = 0x00,
            dlc = 6,
            data = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06)
        )

        whenever(canCommunication.connect()).thenReturn(CommunicationResult.Success(Unit))
        whenever(canCommunication.sendData(testCanData)).thenReturn(CommunicationResult.Success(Unit))

        // When
        viewModel.testCanSend()

        // Then
        verify(canCommunication).connect()
        verify(canCommunication).sendData(testCanData)
    }

}