.PHONY: test-api-parcel

test-api-parcel:
	./tests/api/run.sh $(AUTH_TOKEN) $(BASE_URL)