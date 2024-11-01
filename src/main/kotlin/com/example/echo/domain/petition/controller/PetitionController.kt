package com.example.echo.domain.petition.controller

import com.example.echo.domain.member.repository.MemberRepository
import com.example.echo.domain.petition.dto.request.PetitionRequestDto
import com.example.echo.domain.petition.dto.response.PetitionDetailResponseDto
import com.example.echo.domain.petition.dto.response.PetitionResponseDto
import com.example.echo.domain.petition.entity.Category
import com.example.echo.domain.petition.service.PetitionService
import com.example.echo.global.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.connection.ReactiveStreamCommands.AddStreamRecord.body
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/petitions")
@Tag(name = "Petition Controller", description = "청원 관리 API")
class PetitionController (
    private val petitionService: PetitionService,
    private val memberRepository: MemberRepository
){
    // 청원 등록
    @Operation(summary = "청원 등록", description = "새로운 청원을 등록합니다.")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    fun createPetition(
        @Parameter(description = "청원 등록 요청 정보", required = true) @RequestBody petitionDto: PetitionRequestDto
    ): ResponseEntity<ApiResponse<PetitionDetailResponseDto>> {
        val createdPetition = petitionService!!.createPetition(petitionDto)
        return ResponseEntity.ok(ApiResponse.success(createdPetition))
    }

    // 청원 단건 조회
    @Operation(summary = "청원 단건 조회", description = "특정 ID의 청원을 조회합니다.")
    @GetMapping("/{petitionId}")
    fun getPetitionById(
        @Parameter(description = "조회할 청원의 ID", required = true) @PathVariable petitionId: Long
    ): ResponseEntity<ApiResponse<PetitionDetailResponseDto>> {
        val petition = petitionService!!.getPetitionById(petitionId)
        return ResponseEntity.ok(ApiResponse.success(petition))
    }

    // 청원 전체 조회
    @Operation(summary = "청원 전체 조회", description = "모든 청원을 페이지별로 조회합니다.")
    @GetMapping
    fun getPetitions(
        @Parameter(description = "청원 조회 페이징 요청 정보", required = true) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<PetitionResponseDto>>> {
        val petitions = petitionService!!.getOngoingPetitions(pageable)
        return ResponseEntity.ok(ApiResponse.success(petitions))
    }

    // 청원 카테고리별 조회
    @Operation(summary = "카테고리별 청원 조회", description = "특정 카테고리의 모든 청원을 페이지별로 조회합니다.")
    @GetMapping("/category/{category}")
    fun getPetitionsByCategory(
        @Parameter(description = "조회할 청원의 카테고리", required = true) @PathVariable category: Category,
        @Parameter(description = "청원 조회 페이징 요청 정보", required = true) pageable: Pageable
    ): ResponseEntity<ApiResponse<Page<PetitionResponseDto>>> {
        val petitions = petitionService!!.getPetitionsByCategory(pageable, category)
        return ResponseEntity.ok(ApiResponse.success(petitions))
    }

    @get:GetMapping("/view/endDate")
    @get:Operation(summary = "청원 만료일 기준 조회", description = "만료일이 가까운 청원 5개를 조회합니다.")
    val endDatePetitions: ResponseEntity<ApiResponse<List<PetitionResponseDto>>>
        // 청원 만료일 순 5개 조회
        get() {
            val endDatePetitions = petitionService!!.endDatePetitions
            return ResponseEntity.ok(ApiResponse.success(endDatePetitions))
        }

    @get:GetMapping("/view/likesCount")
    @get:Operation(summary = "청원 좋아요 수 기준 조회", description = "좋아요 수가 많은 청원 5개를 조회합니다.")
    val likesCountPetitions: ResponseEntity<ApiResponse<List<PetitionResponseDto>>>
        // 청원 좋아요 순 5개 조회
        get() {
            val likesCountPetitions = petitionService!!.likesCountPetitions
            return ResponseEntity.ok(ApiResponse.success(likesCountPetitions))
        }

    // 청원 좋아요 기능
    @PreAuthorize("authentication.principal.memberId == #memberId")
    @Operation(summary = "청원 좋아요 토글", description = "청원에 좋아요를 추가하거나 제거합니다.")
    @PostMapping("/{petitionId}/like")
    fun toggleLike(
        @Parameter(description = "좋아요를 추가하거나 제거할 청원의 ID", required = true) @PathVariable petitionId: Long,
        @Parameter(description = "좋아요를 클릭한 회원의 ID", required = true) @RequestParam(required = false) memberId: Long
    ): ResponseEntity<ApiResponse<String>> {
        val message = petitionService!!.toggleLikeOnPetition(petitionId, memberId)
        return ResponseEntity.ok(ApiResponse.success(message))
    }

    // 청원 카테고리 선택 5개 조회
    @Operation(summary = "청원 카테고리별 조회", description = "특정 카테고리의 청원 5개를 랜덤으로 조회합니다.")
    @GetMapping("/view/category/{category}")
    fun getRandomCategoryPetitions(
        @Parameter(description = "랜덤으로 조회할 청원의 카테고리", required = true) @PathVariable category: Category
    ): ResponseEntity<ApiResponse<List<PetitionResponseDto>>> {
        val categoryPetitions = petitionService!!.getRandomCategoryPetitions(category)
        return ResponseEntity.ok(ApiResponse.success(categoryPetitions))
    }

    // 제목으로 청원 검색
    @Operation(summary = "청원 제목으로 검색", description = "제목에 검색어가 포함된 청원을 조회합니다.")
    @GetMapping("/search")
    fun searchPetitions(
        @Parameter(description = "검색할 제목의 키워드", required = true) @RequestParam query: String?
    ): ResponseEntity<ApiResponse<List<PetitionDetailResponseDto>>> {
        val petitions: List<PetitionDetailResponseDto> = petitionService.searchPetitionsByTitle(query)
        return ResponseEntity.ok(ApiResponse.success(petitions))
    }

    // 청원 수정
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "청원 수정", description = "특정 ID의 청원을 수정합니다.")
    @PutMapping("/{petitionId}")
    fun updatePetition(
        @Parameter(description = "수정할 청원의 ID", required = true) @PathVariable petitionId: Long,
        @Parameter(description = "청원 수정 요청 정보", required = true) @RequestBody petitionDto: PetitionRequestDto
    ): ResponseEntity<ApiResponse<PetitionDetailResponseDto>> {
        val updatedPetition = petitionService!!.updatePetition(petitionId, petitionDto)
        return ResponseEntity.ok(ApiResponse.success(updatedPetition))
    }

    // 청원 삭제
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "청원 삭제", description = "특정 ID의 청원을 삭제합니다.")
    @DeleteMapping("/{petitionId}")
    fun deletePetitionById(
        @Parameter(description = "삭제할 청원의 ID", required = true) @PathVariable petitionId: Long
    ): ResponseEntity<ApiResponse<Void>> {
        petitionService!!.deletePetitionById(petitionId)
        return ResponseEntity.noContent().build()
    }


}