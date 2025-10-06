#!/bin/bash

# Python 폴더로 이동
cd "$(dirname "$0")"

# PATH에 uv 추가
export PATH="$HOME/.local/bin:$PATH"

# uv를 사용하여 Streamlit 앱 실행
uv run streamlit run email_agent.py --server.port 8501 --server.address 0.0.0.0