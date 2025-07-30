"""
main_agent.py
main_agent 메인 에이전트-쿼리 라우팅
"""

from main_agent.intent_router import route_intent
from chains import query_analysis_chain, intent_classify_chain
from chains.clarification_chain import get_clarification_chain
from utils.clarification_checker import needs_clarification
from prompts.clarification_prompt import get_clarification_prompt
from config.llm import clarification_llm
from dotenv import load_dotenv

load_dotenv()

# FastAPI 연결용
def run_main_agent(user_input: str):
    query = query_analysis_chain.invoke({"user_input": user_input})
    intent = intent_classify_chain.invoke({"user_input": user_input})

    print("user_input", user_input)
    print("query", query)

    if needs_clarification(intent, query):
        prompt = get_clarification_prompt(intent).format(**query)
        llm_response = clarification_llm.invoke(prompt)
        return {
            "clarification_needed": True,
            "message": llm_response.content,
            "query": query,
            "intent": intent,
        }

    query["user_input"] = user_input
    response = route_intent(intent, query)
    return {
        "clarification_needed": False,
        "response": response,
        "query": query,
        "intent": intent,
    }


#콘솔 테스트용
def run_pipeline():
    while True:
        user_input = input("💬 사용자 질문: ")

        # 1. 질의 분석
        query = query_analysis_chain.invoke({"user_input": user_input})
        print(f"📌 분석 결과: {query}")

        # 2. 화행 분류
        intent = intent_classify_chain.invoke({"user_input": user_input})
        print(f"📌 분류된 의도: {intent}")

        # 3. 필수 정보 누락 시 clarification loop
        while needs_clarification(intent, query):
            # [1] 프롬프트 준비
            prompt = get_clarification_prompt(intent)
            prompt_string = prompt.format(**query)

            # [2] LLM 실행 → 자연어 메시지
            llm_response = clarification_llm.invoke(prompt_string)
            print(f"❓ 추가 질문: {llm_response.content}")  # 여긴 자연어 출력

            user_input = input("↩️ 사용자 응답: ")
            query = query_analysis_chain.invoke({"user_input": user_input})
            print(f"📌 재분석 결과: {query}")

        # 4. 최종 intent 처리
        query["user_input"] = user_input
        response = route_intent(intent, query)
        print(f"\n🤖 챗봇 응답:\n{response}")
        break

if __name__ == "__main__":
    run_pipeline()