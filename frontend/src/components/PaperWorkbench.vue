<template>
  <section class="paper-workbench">
    <div class="workbench-header">
      <div>
        <p class="eyebrow">Paper Workbench</p>
        <h2>论文检索与提交</h2>
      </div>
      <div class="workbench-actions">
        <el-button v-if="isAdmin" :icon="Upload" @click="openImport">arXiv 导入</el-button>
        <el-button v-if="isAdmin" :icon="RefreshRight" @click="openTrash">回收站</el-button>
        <el-button type="primary" :icon="Plus" :disabled="!canSubmit" @click="openCreate">
          提交论文
        </el-button>
      </div>
    </div>

    <el-form class="paper-filter" :model="searchForm" label-position="top" @submit.prevent>
      <el-form-item label="标题">
        <el-input v-model.trim="searchForm.title" clearable placeholder="按标题搜索" />
      </el-form-item>
      <el-form-item label="作者">
        <el-input v-model.trim="searchForm.author" clearable placeholder="按作者搜索" />
      </el-form-item>
      <el-form-item label="摘要关键词">
        <el-input v-model.trim="searchForm.abstractKeyword" clearable placeholder="摘要内容" />
      </el-form-item>
      <el-form-item label="年份">
        <el-input v-model.trim="searchForm.year" clearable placeholder="例如 2026" />
      </el-form-item>
      <el-form-item label="来源">
        <el-input v-model.trim="searchForm.source" clearable placeholder="arXiv / 期刊 / 会议" />
      </el-form-item>
      <el-form-item label="标签">
        <el-select v-model="searchForm.tagId" clearable filterable placeholder="选择标签">
          <el-option
            v-for="option in tagOptions"
            :key="option.id"
            :label="option.label"
            :value="option.id"
          />
        </el-select>
      </el-form-item>
      <div class="filter-actions">
        <el-button type="primary" :icon="Search" :loading="loading" @click="searchPapers">
          搜索
        </el-button>
        <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
      </div>
    </el-form>

    <div class="paper-table-panel">
      <el-table
        v-loading="loading"
        :data="paperPage.items"
        empty-text="暂无论文"
        row-key="id"
        @row-click="openDetail"
      >
        <el-table-column label="标题" min-width="240">
          <template #default="{ row }">
            <div class="paper-title-cell">
              <strong>{{ display(row.title) }}</strong>
              <span>{{ display(row.doi || row.sourcePaperId) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" min-width="180">
          <template #default="{ row }">{{ display(row.authors) }}</template>
        </el-table-column>
        <el-table-column label="年份" width="96">
          <template #default="{ row }">{{ display(row.publishYear) }}</template>
        </el-table-column>
        <el-table-column label="来源" width="140">
          <template #default="{ row }">{{ display(row.source) }}</template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <div class="tag-line">
              <el-tag v-for="tag in row.tags || []" :key="tag.id" size="small" effect="plain">
                {{ tag.name }}
              </el-tag>
              <span v-if="!row.tags || row.tags.length === 0" class="muted-text">未填写</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="引用量" width="100">
          <template #default="{ row }">{{ display(row.citationCount) }}</template>
        </el-table-column>
        <el-table-column label="操作" :width="isAdmin ? 232 : 104" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button type="primary" link :icon="View" @click.stop="openDetail(row)">详情</el-button>
              <template v-if="isAdmin">
                <el-button type="warning" link :icon="Edit" @click.stop="openEdit(row)">编辑</el-button>
                <el-button type="danger" link :icon="Delete" @click.stop="confirmDelete(row)">删除</el-button>
              </template>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <span>共 {{ paperPage.total }} 条</span>
        <el-pagination
          background
          layout="sizes, prev, pager, next"
          :current-page="paperPage.page"
          :page-size="paperPage.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="paperPage.total"
          @current-change="changePage"
          @size-change="changePageSize"
        />
      </div>
    </div>

    <el-drawer v-model="detailVisible" title="论文详情" size="min(640px, 92vw)">
      <div v-if="detail" class="paper-detail">
        <h3>{{ display(detail.title) }}</h3>
        <div class="detail-tags">
          <el-tag v-for="tag in detail.tags || []" :key="tag.id" effect="plain">{{ tag.name }}</el-tag>
          <span v-if="!detail.tags || detail.tags.length === 0" class="muted-text">未填写标签</span>
        </div>

        <div class="detail-actions">
          <el-button :icon="Download" :loading="downloading" :disabled="!canSubmit" @click="attemptDownload">
            尝试下载
          </el-button>
          <template v-if="isAdmin">
            <el-button type="warning" :icon="Edit" @click="openEdit(detail)">编辑</el-button>
            <el-button type="danger" :icon="Delete" @click="confirmDelete(detail)">软删除</el-button>
          </template>
        </div>

        <div v-if="downloadAttempt" class="download-status-panel">
          <div>
            <span>下载状态</span>
            <el-tag :type="downloadStatusType(downloadAttempt.status)">
              {{ display(downloadAttempt.status) }}
            </el-tag>
          </div>
          <dl class="download-status-grid">
            <div>
              <dt>文件名</dt>
              <dd>{{ display(downloadAttempt.fileName) }}</dd>
            </div>
            <div>
              <dt>文件大小</dt>
              <dd>{{ formatFileSize(downloadAttempt.fileSize) }}</dd>
            </div>
            <div>
              <dt>本机路径</dt>
              <dd>{{ display(downloadAttempt.localFilePath) }}</dd>
            </div>
            <div>
              <dt>失败原因</dt>
              <dd>{{ display(downloadAttempt.failureReason) }}</dd>
            </div>
          </dl>
        </div>

        <dl class="detail-grid">
          <div>
            <dt>作者</dt>
            <dd>{{ display(detail.authors) }}</dd>
          </div>
          <div>
            <dt>摘要</dt>
            <dd>{{ display(detail.abstractText) }}</dd>
          </div>
          <div>
            <dt>年份</dt>
            <dd>{{ display(detail.publishYear) }}</dd>
          </div>
          <div>
            <dt>来源</dt>
            <dd>{{ display(detail.source) }}</dd>
          </div>
          <div>
            <dt>来源编号</dt>
            <dd>{{ display(detail.sourcePaperId) }}</dd>
          </div>
          <div>
            <dt>DOI</dt>
            <dd>{{ display(detail.doi) }}</dd>
          </div>
          <div>
            <dt>URL</dt>
            <dd>
              <el-link v-if="detail.sourceUrl" :href="detail.sourceUrl" target="_blank" type="primary">
                {{ detail.sourceUrl }}
              </el-link>
              <span v-else>未填写</span>
            </dd>
          </div>
          <div>
            <dt>下载链接</dt>
            <dd>
              <el-link v-if="detail.downloadUrl" :href="detail.downloadUrl" target="_blank" type="primary">
                {{ detail.downloadUrl }}
              </el-link>
              <span v-else>未填写</span>
            </dd>
          </div>
          <div>
            <dt>关键词</dt>
            <dd>{{ display(detail.keywords) }}</dd>
          </div>
          <div>
            <dt>引用量</dt>
            <dd>{{ display(detail.citationCount) }}</dd>
          </div>
          <div>
            <dt>发布时间</dt>
            <dd>{{ display(detail.publishedAt) }}</dd>
          </div>
        </dl>
      </div>
      <el-empty v-else description="暂无详情" />
    </el-drawer>

    <el-dialog v-model="submitVisible" :title="formDialogTitle" width="min(760px, 92vw)" destroy-on-close>
      <el-form class="submit-form" :model="submitForm" label-position="top">
        <el-form-item label="标题">
          <el-input v-model.trim="submitForm.title" placeholder="论文标题" />
        </el-form-item>
        <el-form-item label="作者">
          <el-input v-model.trim="submitForm.authors" placeholder="作者，多个作者可用逗号分隔" />
        </el-form-item>
        <el-form-item label="摘要" class="wide-field">
          <el-input
            v-model.trim="submitForm.abstractText"
            type="textarea"
            :rows="5"
            placeholder="摘要可以为空"
          />
        </el-form-item>
        <el-form-item label="年份">
          <el-input v-model.trim="submitForm.publishYear" placeholder="例如 2026" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model.trim="submitForm.source" placeholder="arXiv / 期刊 / 会议" />
        </el-form-item>
        <el-form-item label="来源编号">
          <el-input v-model.trim="submitForm.sourcePaperId" placeholder="例如 arXiv ID" />
        </el-form-item>
        <el-form-item label="DOI">
          <el-input v-model.trim="submitForm.doi" placeholder="可为空" />
        </el-form-item>
        <el-form-item label="URL">
          <el-input v-model.trim="submitForm.sourceUrl" placeholder="论文页面 URL" />
        </el-form-item>
        <el-form-item label="下载链接">
          <el-input v-model.trim="submitForm.downloadUrl" placeholder="PDF 或下载 URL" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model.trim="submitForm.keywords" placeholder="关键词，多个可用逗号分隔" />
        </el-form-item>
        <el-form-item label="引用量">
          <el-input v-model.trim="submitForm.citationCount" placeholder="例如 12" />
        </el-form-item>
        <el-form-item label="发布时间">
          <el-date-picker
            v-model="submitForm.publishedAt"
            type="datetime"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择发布时间"
          />
        </el-form-item>
        <el-form-item label="标签" class="wide-field">
          <el-select v-model="submitForm.tagIds" multiple filterable clearable placeholder="选择二级标签">
            <el-option
              v-for="option in tagOptions"
              :key="option.id"
              :label="option.label"
              :value="option.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="savePaper">{{ formSubmitText }}</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="arXiv 导入" width="min(520px, 92vw)">
      <el-form class="import-form" :model="importForm" label-position="top">
        <el-form-item label="查询语句">
          <el-input v-model.trim="importForm.query" placeholder="留空使用默认查询" />
        </el-form-item>
        <el-form-item label="导入数量">
          <el-input v-model.trim="importForm.maxResults" placeholder="100" />
        </el-form-item>
      </el-form>
      <div v-if="importResult" class="import-result">
        <div>
          <span>请求</span>
          <strong>{{ importResult.requested }}</strong>
        </div>
        <div>
          <span>导入</span>
          <strong>{{ importResult.imported }}</strong>
        </div>
        <div>
          <span>跳过</span>
          <strong>{{ importResult.skipped }}</strong>
        </div>
        <div>
          <span>失败</span>
          <strong>{{ importResult.failed }}</strong>
        </div>
        <p>{{ display(importResult.message) }}</p>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">关闭</el-button>
        <el-button type="primary" :loading="importing" @click="runImport">开始导入</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="trashVisible" title="论文回收站" size="min(760px, 96vw)" @opened="loadDeletedPapers">
      <el-table v-loading="deletedLoading" :data="deletedPage.items" row-key="id" empty-text="暂无已删除论文">
        <el-table-column label="论文" min-width="260">
          <template #default="{ row }">
            <div class="trash-title-cell">
              <strong>{{ display(row.title) }}</strong>
              <span>{{ display(row.source) }} · {{ display(row.publishYear) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" min-width="160">
          <template #default="{ row }">{{ display(row.authors) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              link
              :icon="RefreshRight"
              :loading="restoringId === row.id"
              @click="restoreDeleted(row)"
            >
              恢复
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-row">
        <span>共 {{ deletedPage.total }} 条</span>
        <el-pagination
          background
          layout="sizes, prev, pager, next"
          :current-page="deletedPage.page"
          :page-size="deletedPage.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="deletedPage.total"
          @current-change="changeDeletedPage"
          @size-change="changeDeletedPageSize"
        />
      </div>
    </el-drawer>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Download, Edit, Plus, Refresh, RefreshRight, Search, Upload, View } from '@element-plus/icons-vue'
import {
  attemptPaperDownload,
  createPaper,
  fetchDeletedPapers,
  fetchPaperDetail,
  fetchPapers,
  fetchTags,
  importArxivPapers,
  restorePaper,
  softDeletePaper,
  updatePaper
} from '../api'

const props = defineProps({
  currentUser: {
    type: Object,
    default: null
  }
})

const canSubmit = computed(() => Boolean(props.currentUser))
const isAdmin = computed(() => props.currentUser?.role === 'ADMIN')
const formDialogTitle = computed(() => (formMode.value === 'edit' ? '编辑论文' : '提交论文'))
const formSubmitText = computed(() => (formMode.value === 'edit' ? '保存' : '提交'))

const loading = ref(false)
const submitting = ref(false)
const downloading = ref(false)
const importing = ref(false)
const deletedLoading = ref(false)
const restoringId = ref(null)
const detailVisible = ref(false)
const submitVisible = ref(false)
const importVisible = ref(false)
const trashVisible = ref(false)
const formMode = ref('create')
const editingPaperId = ref(null)
const detail = ref(null)
const downloadAttempt = ref(null)
const importResult = ref(null)
const tagTree = ref([])

const searchForm = reactive({
  title: '',
  author: '',
  abstractKeyword: '',
  year: '',
  source: '',
  tagId: null
})

const paperPage = reactive({
  items: [],
  total: 0,
  page: 1,
  pageSize: 10
})

const deletedPage = reactive({
  items: [],
  total: 0,
  page: 1,
  pageSize: 10
})

const submitForm = reactive(createEmptySubmitForm())

const importForm = reactive({
  query: '',
  maxResults: '100'
})

const tagOptions = computed(() => {
  const options = []
  tagTree.value.forEach((parent) => {
    const children = parent.children || []
    if (children.length === 0) {
      options.push({ id: parent.id, label: parent.name })
      return
    }
    children.forEach((child) => {
      options.push({ id: child.id, label: `${parent.name} / ${child.name}` })
    })
  })
  return options
})

onMounted(async () => {
  await Promise.all([loadTags(), loadPapers()])
})

async function loadTags() {
  try {
    tagTree.value = await fetchTags()
  } catch (error) {
    ElMessage.error(error.message || '标签加载失败')
  }
}

async function loadPapers() {
  loading.value = true
  try {
    const data = await fetchPapers({
      ...buildSearchParams(),
      page: paperPage.page,
      pageSize: paperPage.pageSize
    })
    paperPage.items = data.items || []
    paperPage.total = data.total || 0
    paperPage.page = data.page || paperPage.page
    paperPage.pageSize = data.pageSize || paperPage.pageSize
  } catch (error) {
    ElMessage.error(error.message || '论文列表加载失败')
  } finally {
    loading.value = false
  }
}

async function loadDeletedPapers() {
  if (!isAdmin.value) {
    return
  }
  deletedLoading.value = true
  try {
    const data = await fetchDeletedPapers({
      page: deletedPage.page,
      pageSize: deletedPage.pageSize
    })
    deletedPage.items = data.items || []
    deletedPage.total = data.total || 0
    deletedPage.page = data.page || deletedPage.page
    deletedPage.pageSize = data.pageSize || deletedPage.pageSize
  } catch (error) {
    ElMessage.error(error.message || '回收站加载失败')
  } finally {
    deletedLoading.value = false
  }
}

async function openDetail(row) {
  detailVisible.value = true
  detail.value = null
  downloadAttempt.value = null
  try {
    detail.value = await fetchPaperDetail(row.id)
  } catch (error) {
    ElMessage.error(error.message || '论文详情加载失败')
  }
}

function openCreate() {
  formMode.value = 'create'
  editingPaperId.value = null
  Object.assign(submitForm, createEmptySubmitForm())
  submitVisible.value = true
}

async function openEdit(row) {
  if (!isAdmin.value) {
    return
  }
  try {
    const paper = await fetchPaperDetail(row.id)
    formMode.value = 'edit'
    editingPaperId.value = paper.id
    fillSubmitForm(paper)
    submitVisible.value = true
  } catch (error) {
    ElMessage.error(error.message || '论文详情加载失败')
  }
}

async function savePaper() {
  submitting.value = true
  try {
    const payload = normalizeSubmitForm()
    if (formMode.value === 'edit') {
      const updated = await updatePaper(editingPaperId.value, payload)
      ElMessage.success('论文已保存')
      submitVisible.value = false
      await loadPapers()
      if (detailVisible.value && detail.value?.id === updated.id) {
        detail.value = updated
      }
      return
    }

    const created = await createPaper(payload)
    ElMessage.success('论文已提交')
    submitVisible.value = false
    Object.assign(submitForm, createEmptySubmitForm())
    paperPage.page = 1
    await loadPapers()
    if (created?.id) {
      await openDetail(created)
    }
  } catch (error) {
    ElMessage.error(error.message || '论文保存失败')
  } finally {
    submitting.value = false
  }
}

async function confirmDelete(row) {
  if (!isAdmin.value) {
    return
  }
  try {
    await ElMessageBox.confirm(`确认将“${display(row.title)}”移入回收站？`, '软删除论文', {
      type: 'warning',
      confirmButtonText: '软删除',
      cancelButtonText: '取消'
    })
    await softDeletePaper(row.id)
    ElMessage.success('论文已移入回收站')
    if (detail.value?.id === row.id) {
      detailVisible.value = false
      detail.value = null
    }
    await loadPapers()
    if (trashVisible.value) {
      await loadDeletedPapers()
    }
  } catch (error) {
    if (error !== 'cancel' && error !== 'close') {
      ElMessage.error(error.message || '软删除失败')
    }
  }
}

async function restoreDeleted(row) {
  restoringId.value = row.id
  try {
    await restorePaper(row.id)
    ElMessage.success('论文已恢复')
    await Promise.all([loadPapers(), loadDeletedPapers()])
  } catch (error) {
    ElMessage.error(error.message || '恢复失败')
  } finally {
    restoringId.value = null
  }
}

async function attemptDownload() {
  if (!detail.value) {
    return
  }
  downloading.value = true
  try {
    downloadAttempt.value = await attemptPaperDownload(detail.value.id)
    if (downloadAttempt.value.status === 'SUCCESS') {
      ElMessage.success('下载成功')
    } else {
      ElMessage.warning(`下载结果: ${display(downloadAttempt.value.status)}`)
    }
  } catch (error) {
    ElMessage.error(error.message || '下载尝试失败')
  } finally {
    downloading.value = false
  }
}

function openImport() {
  importResult.value = null
  importVisible.value = true
}

async function runImport() {
  importing.value = true
  try {
    importResult.value = await importArxivPapers({
      query: blankToNull(importForm.query),
      maxResults: toNumberOrNull(importForm.maxResults) || 100
    })
    ElMessage.success('导入任务完成')
    paperPage.page = 1
    await loadPapers()
  } catch (error) {
    ElMessage.error(error.message || 'arXiv 导入失败')
  } finally {
    importing.value = false
  }
}

function openTrash() {
  trashVisible.value = true
  loadDeletedPapers()
}

function searchPapers() {
  paperPage.page = 1
  loadPapers()
}

function resetSearch() {
  Object.assign(searchForm, {
    title: '',
    author: '',
    abstractKeyword: '',
    year: '',
    source: '',
    tagId: null
  })
  paperPage.page = 1
  loadPapers()
}

function changePage(page) {
  paperPage.page = page
  loadPapers()
}

function changePageSize(size) {
  paperPage.pageSize = size
  paperPage.page = 1
  loadPapers()
}

function changeDeletedPage(page) {
  deletedPage.page = page
  loadDeletedPapers()
}

function changeDeletedPageSize(size) {
  deletedPage.pageSize = size
  deletedPage.page = 1
  loadDeletedPapers()
}

function buildSearchParams() {
  return {
    title: searchForm.title,
    author: searchForm.author,
    abstractKeyword: searchForm.abstractKeyword,
    year: toNumberOrNull(searchForm.year),
    source: searchForm.source,
    tagId: searchForm.tagId
  }
}

function normalizeSubmitForm() {
  return {
    title: blankToNull(submitForm.title),
    authors: blankToNull(submitForm.authors),
    abstractText: blankToNull(submitForm.abstractText),
    publishYear: toNumberOrNull(submitForm.publishYear),
    source: blankToNull(submitForm.source),
    sourcePaperId: blankToNull(submitForm.sourcePaperId),
    doi: blankToNull(submitForm.doi),
    sourceUrl: blankToNull(submitForm.sourceUrl),
    downloadUrl: blankToNull(submitForm.downloadUrl),
    keywords: blankToNull(submitForm.keywords),
    citationCount: toNumberOrNull(submitForm.citationCount),
    publishedAt: blankToNull(submitForm.publishedAt),
    tagIds: submitForm.tagIds
  }
}

function fillSubmitForm(paper) {
  Object.assign(submitForm, {
    title: paper.title || '',
    authors: paper.authors || '',
    abstractText: paper.abstractText || '',
    publishYear: paper.publishYear === null || paper.publishYear === undefined ? '' : String(paper.publishYear),
    source: paper.source || '',
    sourcePaperId: paper.sourcePaperId || '',
    doi: paper.doi || '',
    sourceUrl: paper.sourceUrl || '',
    downloadUrl: paper.downloadUrl || '',
    keywords: paper.keywords || '',
    citationCount: paper.citationCount === null || paper.citationCount === undefined ? '' : String(paper.citationCount),
    publishedAt: paper.publishedAt || '',
    tagIds: (paper.tags || []).map((tag) => tag.id)
  })
}

function createEmptySubmitForm() {
  return {
    title: '',
    authors: '',
    abstractText: '',
    publishYear: '',
    source: '',
    sourcePaperId: '',
    doi: '',
    sourceUrl: '',
    downloadUrl: '',
    keywords: '',
    citationCount: '',
    publishedAt: '',
    tagIds: []
  }
}

function blankToNull(value) {
  return value === undefined || value === null || value === '' ? null : value
}

function toNumberOrNull(value) {
  if (value === undefined || value === null || value === '') {
    return null
  }
  const numberValue = Number(value)
  return Number.isFinite(numberValue) ? numberValue : null
}

function display(value) {
  return value === undefined || value === null || value === '' ? '未填写' : value
}

function formatFileSize(value) {
  if (value === undefined || value === null) {
    return '未填写'
  }
  if (value < 1024) {
    return `${value} B`
  }
  if (value < 1024 * 1024) {
    return `${(value / 1024).toFixed(1)} KB`
  }
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}

function downloadStatusType(status) {
  if (status === 'SUCCESS') {
    return 'success'
  }
  if (status === 'NO_URL' || status === 'NON_PDF') {
    return 'warning'
  }
  if (status === 'FAILED' || status === 'TIMEOUT') {
    return 'danger'
  }
  return 'info'
}
</script>
