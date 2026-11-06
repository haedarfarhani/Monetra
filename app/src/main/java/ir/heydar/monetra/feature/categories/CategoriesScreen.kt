package ir.heydar.monetra.feature.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ir.heydar.monetra.R
import ir.heydar.monetra.core.designsystem.components.AppTextField
import ir.heydar.monetra.core.designsystem.components.AppTopBar
import ir.heydar.monetra.core.designsystem.components.EmptyState
import ir.heydar.monetra.core.designsystem.components.GlassFilterChip
import ir.heydar.monetra.core.designsystem.components.LoadingIndicator
import ir.heydar.monetra.core.designsystem.components.MonetraCategoryIcon
import ir.heydar.monetra.core.designsystem.glass.GlassSurface
import ir.heydar.monetra.core.designsystem.theme.Dimens
import ir.heydar.monetra.core.designsystem.theme.glassColors
import ir.heydar.monetra.core.designsystem.theme.monetraColors
import ir.heydar.monetra.domain.model.Category
import ir.heydar.monetra.domain.model.TransactionType
import ir.heydar.monetra.feature.dashboard.typeLabelRes
import kotlinx.coroutines.delay

@Composable
fun CategoriesRoute(
    viewModel: CategoriesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    CategoriesScreen(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
fun CategoriesScreen(
    uiState: CategoriesUiState = CategoriesUiState(),
    onEvent: (CategoriesUiEvent) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filteredCategories = remember(uiState.categories, uiState.selectedType) {
        uiState.categories.filter { it.type == uiState.selectedType }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            AppTopBar(title = stringResource(id = R.string.categories_title))
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .clickable { onEvent(CategoriesUiEvent.AddCategoryClicked) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(id = R.string.action_new_category),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        modifier = modifier
    ) { innerPadding ->
        if (uiState.isLoading) {
            LoadingIndicator(modifier = Modifier.padding(innerPadding))
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.screenPadding, vertical = Dimens.spacingSm)
        ) {
            // Type toggle chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(TransactionType.EXPENSE, TransactionType.INCOME).forEach { type ->
                    GlassFilterChip(
                        selected = uiState.selectedType == type,
                        onClick = { onEvent(CategoriesUiEvent.ChangeType(type)) },
                        label = { Text(stringResource(id = typeLabelRes(type))) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSm))

            if (filteredCategories.isEmpty()) {
                EmptyState(
                    message = stringResource(id = R.string.no_categories),
                    modifier = Modifier.weight(1f)
                )
            } else {
                CategoryList(
                    categories = filteredCategories,
                    onEdit = { onEvent(CategoriesUiEvent.EditCategoryClicked(it)) },
                    onDelete = { onEvent(CategoriesUiEvent.DeleteRequested(it)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (uiState.isDialogVisible) {
        CategoryEditorDialog(
            editingCategory = uiState.editingCategory,
            initialType = uiState.selectedType,
            onDismiss = { onEvent(CategoriesUiEvent.DismissDialog) },
            onConfirm = { name, type ->
                onEvent(CategoriesUiEvent.SubmitCategory(name, type))
            }
        )
    }

    uiState.categoryToDelete?.let { category ->
        AlertDialog(
            onDismissRequest = { onEvent(CategoriesUiEvent.DismissDeleteConfirm) },
            containerColor = MaterialTheme.glassColors.dialogFillTop,
            title = { Text(stringResource(id = R.string.category_delete_title)) },
            text = {
                Text(
                    text = stringResource(id = R.string.category_delete_message) + "\n\n" + category.name,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Text(
                    text = stringResource(id = R.string.action_delete),
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onEvent(CategoriesUiEvent.DeleteConfirmed) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            },
            dismissButton = {
                Text(
                    text = stringResource(id = R.string.action_cancel),
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onEvent(CategoriesUiEvent.DismissDeleteConfirm) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        )
    }
}

@Composable
private fun CategoryList(
    categories: List<Category>,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // --- Discoverability hint: gently sway the first card once. ---
    var showHint by remember { mutableStateOf(false) }
    val hintOffset = remember { Animatable(0f) }
    val density = LocalDensity.current
    LaunchedEffect(categories.size) {
        if (categories.isEmpty()) return@LaunchedEffect
        delay(600)
        showHint = true
        val distance = with(density) { 44.dp.toPx() }
        val sway = tween<Float>(450, easing = FastOutSlowInEasing)
        hintOffset.animateTo(distance, sway)
        hintOffset.animateTo(-distance, tween(650, easing = FastOutSlowInEasing))
        hintOffset.animateTo(distance / 2f, sway)
        hintOffset.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        showHint = false
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "hint") {
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlassSurface(
                        shape = RoundedCornerShape(50),
                        showSheen = false
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = stringResource(id = R.string.accounts_swipe_hint),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        items(categories, key = { it.id }) { category ->
            val isFirst = category.id == categories.first().id
            SwipeableCategoryItem(
                category = category,
                modifier = if (isFirst) {
                    Modifier.graphicsLayer { translationX = hintOffset.value }
                } else {
                    Modifier
                },
                onEdit = { onEdit(category.id) },
                onDelete = { onDelete(category.id) }
            )
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableCategoryItem(
    category: Category,
    modifier: Modifier = Modifier,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    // System categories can be renamed but never deleted.
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onEdit()
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    if (!category.isSystem) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete()
                    }
                }
                SwipeToDismissBoxValue.Settled -> Unit
            }
            // Always snap back into place; the action itself is handled above.
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = !category.isSystem,
        backgroundContent = {
            val target = dismissState.targetValue
            val extendedColors = MaterialTheme.monetraColors
            val settled = target == SwipeToDismissBoxValue.Settled
            val backgroundColor by animateColorAsState(
                targetValue = when {
                    settled -> Color.Transparent
                    target == SwipeToDismissBoxValue.StartToEnd -> extendedColors.success
                    else -> extendedColors.expense
                },
                animationSpec = tween(180),
                label = "swipeBg"
            )
            val contentAlpha by animateFloatAsState(
                targetValue = if (settled) 0f else 1f,
                animationSpec = tween(180),
                label = "swipeContent"
            )
            val (icon, alignment) = when (target) {
                SwipeToDismissBoxValue.StartToEnd -> Icons.Outlined.Edit to Alignment.CenterStart
                else -> Icons.Outlined.Delete to Alignment.CenterEnd
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .background(backgroundColor),
                contentAlignment = alignment
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = stringResource(
                            id = if (target == SwipeToDismissBoxValue.StartToEnd) {
                                R.string.action_edit
                            } else {
                                R.string.action_delete
                            }
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    ) {
        CategoryItem(category = category, onClick = onEdit)
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    onClick: () -> Unit
) {
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonetraCategoryIcon(
                categoryName = category.name,
                transactionType = category.type,
                size = 42.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (category.isSystem) {
                        Surface(shape = RoundedCornerShape(50), color = MaterialTheme.glassColors.innerFillTop) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(10.dp)
                                )
                                Text(
                                    text = stringResource(id = R.string.category_system_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                Text(
                    text = stringResource(id = typeLabelRes(category.type)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/** Create/edit dialog. The type is selectable on create and locked on edit. */
@Composable
private fun CategoryEditorDialog(
    editingCategory: Category?,
    initialType: TransactionType,
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: TransactionType) -> Unit
) {
    val isEditMode = editingCategory != null
    var name by remember(editingCategory?.id) {
        mutableStateOf(editingCategory?.name ?: "")
    }
    var type by remember(editingCategory?.id) {
        mutableStateOf(editingCategory?.type ?: initialType)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.glassColors.dialogFillTop,
        title = {
            Text(
                text = stringResource(
                    id = if (isEditMode) R.string.category_edit_title else R.string.action_new_category
                ),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                AppTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(id = R.string.field_category_name)
                )

                if (isEditMode) {
                    // Type is locked while editing to keep historical transactions coherent.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.detail_type),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(id = typeLabelRes(type)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassFilterChip(
                            selected = type == TransactionType.EXPENSE,
                            onClick = { type = TransactionType.EXPENSE },
                            label = { Text(stringResource(id = typeLabelRes(TransactionType.EXPENSE))) }
                        )
                        GlassFilterChip(
                            selected = type == TransactionType.INCOME,
                            onClick = { type = TransactionType.INCOME },
                            label = { Text(stringResource(id = typeLabelRes(TransactionType.INCOME))) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Text(
                text = stringResource(id = R.string.action_save),
                color = if (name.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = name.isNotBlank()) { onConfirm(name.trim(), type) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        },
        dismissButton = {
            Text(
                text = stringResource(id = R.string.action_cancel),
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    )
}
