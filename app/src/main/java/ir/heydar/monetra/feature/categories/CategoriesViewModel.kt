package ir.heydar.monetra.feature.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.heydar.monetra.core.common.DispatcherProvider
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoriesUiState(
    val isLoading: Boolean = false,
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val categories: List<Category> = emptyList(),
    /** Non-null while the editor dialog edits an existing category. */
    val editingCategory: Category? = null,
    val isDialogVisible: Boolean = false,
    /** Non-null while the delete confirmation dialog is shown. */
    val categoryToDelete: Category? = null
)

sealed interface CategoriesUiEvent {
    data class ChangeType(val type: TransactionType) : CategoriesUiEvent
    data object AddCategoryClicked : CategoriesUiEvent
    data object DismissDialog : CategoriesUiEvent
    data class EditCategoryClicked(val id: Long) : CategoriesUiEvent
    data class SubmitCategory(val name: String, val type: TransactionType) : CategoriesUiEvent
    data class DeleteRequested(val id: Long) : CategoriesUiEvent
    data object DeleteConfirmed : CategoriesUiEvent
    data object DismissDeleteConfirm : CategoriesUiEvent
}

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoriesUiState(isLoading = true))
    val uiState: StateFlow<CategoriesUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    fun onEvent(event: CategoriesUiEvent) {
        when (event) {
            is CategoriesUiEvent.ChangeType -> {
                _uiState.update { it.copy(selectedType = event.type) }
            }
            is CategoriesUiEvent.AddCategoryClicked -> _uiState.update {
                it.copy(isDialogVisible = true, editingCategory = null)
            }
            is CategoriesUiEvent.DismissDialog -> _uiState.update {
                it.copy(isDialogVisible = false, editingCategory = null)
            }
            is CategoriesUiEvent.EditCategoryClicked -> _uiState.update { state ->
                state.copy(
                    isDialogVisible = true,
                    editingCategory = state.categories.firstOrNull { it.id == event.id }
                )
            }
            is CategoriesUiEvent.SubmitCategory -> submitCategory(event)
            is CategoriesUiEvent.DeleteRequested -> _uiState.update { state ->
                state.copy(categoryToDelete = state.categories.firstOrNull { it.id == event.id })
            }
            is CategoriesUiEvent.DismissDeleteConfirm -> _uiState.update {
                it.copy(categoryToDelete = null)
            }
            is CategoriesUiEvent.DeleteConfirmed -> deleteCategory()
        }
    }

    private fun submitCategory(event: CategoriesUiEvent.SubmitCategory) {
        val name = event.name.trim()
        if (name.isEmpty()) return
        val editing = _uiState.value.editingCategory

        viewModelScope.launch(dispatcherProvider.io) {
            if (editing == null) {
                categoryRepository.createCategory(
                    Category(name = name, type = event.type, isSystem = false)
                )
            } else {
                categoryRepository.updateCategory(editing.copy(name = name))
            }
            _uiState.update {
                it.copy(isDialogVisible = false, editingCategory = null)
            }
        }
    }

    private fun deleteCategory() {
        val category = _uiState.value.categoryToDelete ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            runCatching { categoryRepository.deleteCategory(category) }
            _uiState.update { it.copy(categoryToDelete = null) }
        }
    }

    private fun loadCategories() {
        viewModelScope.launch(dispatcherProvider.io) {
            categoryRepository.getAllCategories().collect { allCategories ->
                _uiState.update { it.copy(isLoading = false, categories = allCategories) }
            }
        }
    }
}
