package org.poten.backend.global.service;

import org.poten.backend.global.error.ErrorCode;

/*
 모든 도메인 서비스가 구현해야 하는 최상위 인터페이스
 */
public interface DomainService<E extends Enum<E> & ErrorCode> {
    /*
    해당 인터페이스로 구현하는 클래스는 내부에 ErrorCode를 구현한 Enum을 가지고 있어야 합니다.
    -> 통일성 있게 에러 코드를 반환하기 위해 해당 서비스에 발생 가능한 에러 코드 정의를 강제하였습니다 !
     */
}
