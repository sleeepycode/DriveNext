package ru.mtuci.drivenext

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import ru.mtuci.drivenext.domain.model.Car
import ru.mtuci.drivenext.domain.model.CatalogQuery
import ru.mtuci.drivenext.domain.model.UserProfile
import ru.mtuci.drivenext.domain.repository.CatalogRepository
import ru.mtuci.drivenext.domain.usecase.CatalogUseCases
import ru.mtuci.drivenext.presentation.main.CatalogViewModel

@OptIn(ExperimentalCoroutinesApi::class)
class CatalogTest {
    private class Fake : CatalogRepository {
        var query = ""
        var lastQuery = ""
        var fail = false
        var uid: String? = "test"
        val item = Car(1, "BMW", "530i", 2900, "AT", "Бензин", "sedan_blue")
        override fun userId() = uid
        override fun savedQuery() = query
        override fun saveQuery(query: String) { this.query = query }
        override suspend fun cars(query: String): List<Car> {
            lastQuery = query
            check(!fail)
            return listOf(item).filter { "${it.brand} ${it.model}".contains(query, true) }
        }
        override suspend fun profile() = UserProfile("test", "test@domain.ru", "Иван", "Иванов", "male", "email", null, "2026-01-01")
        override suspend fun avatar(path: String) = byteArrayOf(1, 2)
        override suspend fun updateAvatar(uri: String) = profile().copy(avatarPath = "test/avatar.jpg")
        override suspend fun signOut() { uid = null }
    }
    @Before fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @After fun cleanup() { Dispatchers.resetMain() }
    @Test fun queryNormalized() { assertEquals("BMW 530i", CatalogQuery.normalize(" BMW   530i ")) }
    @Test fun queryLengthLimited() { assertEquals(100, CatalogQuery.normalize("x".repeat(120)).length) }
    @Test fun profileNameFallback() { assertEquals("a@b.ru", UserProfile("x", "a@b.ru", "", "", "", "google", null, "").displayName) }
    @Test fun initialCatalog() = runTest {
        val repo = Fake(); val vm = CatalogViewModel(CatalogUseCases(repo)); vm.open(); advanceUntilIdle()
        assertEquals(CatalogViewModel.Screen.HOME, vm.ui.value.screen); assertEquals(1, vm.ui.value.cars.size)
        assertFalse(vm.ui.value.busy)
    }
    @Test fun searchUpdatesAndPersistsQuery() = runTest {
        val repo = Fake(); val vm = CatalogViewModel(CatalogUseCases(repo)); vm.open(); advanceUntilIdle()
        vm.search("B"); vm.search("BMW"); advanceUntilIdle()
        assertEquals("BMW", repo.lastQuery); assertEquals("BMW", repo.query); assertEquals(CatalogViewModel.Screen.SEARCH, vm.ui.value.screen)
    }
    @Test fun searchRestoredOnLaunch() = runTest {
        val repo = Fake().apply { query = "530i" }; val vm = CatalogViewModel(CatalogUseCases(repo)); vm.open(); advanceUntilIdle()
        assertEquals(CatalogViewModel.Screen.SEARCH, vm.ui.value.screen); assertEquals("530i", repo.lastQuery)
    }
    @Test fun emptySearchResults() = runTest {
        val vm = CatalogViewModel(CatalogUseCases(Fake())); vm.open(); vm.search("unknown"); advanceUntilIdle()
        assertTrue(vm.ui.value.cars.isEmpty()); assertNull(vm.ui.value.error)
    }
    @Test fun errorAndRetry() = runTest {
        val repo = Fake().apply { fail = true }; val vm = CatalogViewModel(CatalogUseCases(repo)); vm.open(); advanceUntilIdle()
        assertNotNull(vm.ui.value.error); repo.fail = false; vm.retry(); advanceUntilIdle(); assertNull(vm.ui.value.error); assertEquals(1, vm.ui.value.cars.size)
    }
    @Test fun profileAvatarUpdated() = runTest {
        val vm = CatalogViewModel(CatalogUseCases(Fake())); vm.open(); advanceUntilIdle(); vm.profile(); advanceUntilIdle()
        vm.updateAvatar("content://test/photo"); advanceUntilIdle(); assertEquals("test/avatar.jpg", vm.ui.value.profile?.avatarPath)
        assertArrayEquals(byteArrayOf(1,2), vm.ui.value.avatar)
    }
    @Test fun logoutClearsState() = runTest {
        val repo = Fake(); val vm = CatalogViewModel(CatalogUseCases(repo)); vm.open(); advanceUntilIdle(); vm.signOut(); advanceUntilIdle()
        assertNull(repo.uid); assertTrue(vm.ui.value.signedOut); assertTrue(vm.ui.value.cars.isEmpty())
    }
}
