package com.docopener.universal

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.docopener.universal.data.RecentDocsRepository
import com.docopener.universal.data.StorageScanner
import com.docopener.universal.domain.DocumentItem
import com.docopener.universal.engine.DocTypeDetector
import com.docopener.universal.ui.screens.HomeScreen
import com.docopener.universal.ui.screens.UniversalViewerContainer
import com.docopener.universal.ui.theme.UniversalDocTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var recentRepo: RecentDocsRepository
    private lateinit var storageScanner: StorageScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        recentRepo = RecentDocsRepository(this)
        storageScanner = StorageScanner(this)

        setContent {
            UniversalDocTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var documents by remember { mutableStateOf<List<DocumentItem>>(emptyList()) }
                    var isLoading by remember { mutableStateOf(true) }
                    var activeDoc by remember { mutableStateOf<DocumentItem?>(null) }
                    val scope = rememberCoroutineScope()

                    fun refreshDocuments() {
                        scope.launch {
                            isLoading = true
                            documents = storageScanner.scanDocuments()
                            isLoading = false
                        }
                    }

                    fun openDocument(item: DocumentItem) {
                        recentRepo.addRecentDocument(item)
                        activeDoc = item
                    }

                    // Permission launcher for storage access
                    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { _ ->
                        refreshDocuments()
                    }

                    LaunchedEffect(Unit) {
                        checkAndRequestPermissions { permissions ->
                            if (permissions.isNotEmpty()) {
                                permissionLauncher.launch(permissions.toTypedArray())
                            } else {
                                refreshDocuments()
                            }
                        }
                    }

                    // Check if launched with an Intent from external app (WhatsApp, File Manager, Email)
                    LaunchedEffect(intent) {
                        handleIncomingIntent(intent)?.let { item ->
                            openDocument(item)
                        }
                    }

                    var isCreatingPdf by remember { mutableStateOf(false) }

                    if (activeDoc != null) {
                        UniversalViewerContainer(
                            doc = activeDoc!!,
                            onBack = { activeDoc = null }
                        )
                    } else if (isCreatingPdf) {
                        com.docopener.universal.ui.screens.CreatePdfScreen(
                            onBack = { isCreatingPdf = false },
                            onPdfCreated = { createdDoc ->
                                isCreatingPdf = false
                                refreshDocuments()
                                openDocument(createdDoc)
                            }
                        )
                    } else {
                        HomeScreen(
                            documents = documents,
                            isLoading = isLoading,
                            recentRepo = recentRepo,
                            onOpenDoc = { doc -> openDocument(doc) },
                            onRefresh = { refreshDocuments() },
                            onNavigateToCreatePdf = { isCreatingPdf = true },
                            onPickFile = { uri ->
                                val name = DocTypeDetector.getFileName(this@MainActivity, uri)
                                val size = DocTypeDetector.getFileSize(this@MainActivity, uri)
                                val type = DocTypeDetector.detectType(this@MainActivity, uri)
                                val item = DocumentItem(
                                    uri = uri,
                                    name = name,
                                    sizeBytes = size,
                                    lastModified = System.currentTimeMillis(),
                                    type = type
                                )
                                openDocument(item)
                            },
                            onShare = { doc ->
                                try {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "*/*"
                                        putExtra(Intent.EXTRA_STREAM, doc.uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    startActivity(Intent.createChooser(shareIntent, "Share Document"))
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?): DocumentItem? {
        if (intent == null) return null
        val action = intent.action
        val data: Uri? = intent.data ?: intent.getParcelableExtra(Intent.EXTRA_STREAM)

        if (data != null && (Intent.ACTION_VIEW == action || Intent.ACTION_SEND == action)) {
            val name = DocTypeDetector.getFileName(this, data)
            val size = DocTypeDetector.getFileSize(this, data)
            val type = DocTypeDetector.detectType(this, data)
            return DocumentItem(
                uri = data,
                name = name,
                sizeBytes = size,
                lastModified = System.currentTimeMillis(),
                type = type
            )
        }
        return null
    }

    private fun checkAndRequestPermissions(onNeedPermissions: (List<String>) -> Unit) {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        onNeedPermissions(needed)
    }
}
