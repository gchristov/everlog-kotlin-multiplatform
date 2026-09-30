FROM python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY bq_query.py .

ENV PYTHONUNBUFFERED=1

ENTRYPOINT ["python", "/app/bq_query.py"]
