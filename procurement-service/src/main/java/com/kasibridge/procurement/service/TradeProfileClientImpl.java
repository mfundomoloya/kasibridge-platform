package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TraderProfileClientResponse;
import com.kasibridge.procurement.exception.SupportTicketException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeProfileClientImpl implements TraderProfileClient {

    private final RestTemplate restTemplate;

    @Value("${kasibridge.services.trader-profile.base-url}")
    private String traderProfileBaseUrl;

    @Override
    public TraderProfileClientResponse getTraderProfileByUserId(Long userId) {

        String url = traderProfileBaseUrl + "/api/v1/traders/user/" + userId;

        try {

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(extractBearerToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<TraderProfileClientResponse> response =
            restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TraderProfileClientResponse.class
            );

            if (response.getBody() == null) {
                throw new SupportTicketException("Trader profile not found for user ID: " + userId);
            }

            return response.getBody();

        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Forbidden calling trader-profile-service for userId={}", userId, ex);

            throw new SupportTicketException("Not allowed to access trader profile for user ID: " + userId);

        } catch (Exception ex) {

            log.error(
                    "Failed to load trader profile for userId={} from url={}",
                    userId,
                    url,
                    ex
            );

            throw new SupportTicketException("Unable to load trader profile for user ID: " + userId);
        }
    }

    @Override
    public TraderProfileClientResponse getTraderProfileById(Long traderId) {
        String url = traderProfileBaseUrl + "/api/v1/traders/" + traderId;

        try {

            HttpHeaders headers = new HttpHeaders();

            headers.setBearerAuth(extractBearerToken());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<TraderProfileClientResponse> response =
            restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    TraderProfileClientResponse.class
            );

            if (response.getBody() == null) {

                throw new SupportTicketException("Trader profile not found with ID: " + traderId);
            }

            return response.getBody();

        } catch (HttpClientErrorException.NotFound ex) {

            log.error("Trader profile not found for traderId={}", traderId, ex);

            throw new SupportTicketException("Trader profile not found with ID: " + traderId);

        } catch (HttpClientErrorException.Unauthorized ex) {

            log.error("Unauthorized calling trader-profile-service for traderId={}", traderId, ex);

            throw new SupportTicketException("Unable to authenticate with trader profile service.");

        } catch (HttpClientErrorException.Forbidden ex) {
            log.error("Forbidden calling trader-profile-service for traderId={}", traderId, ex);

            throw new SupportTicketException("Not allowed to access trader profile ID: " + traderId);

        } catch (Exception ex) {
            log.error(
                    "Failed to load trader profile for traderId={} from url={}",
                    traderId,
                    url,
                    ex
            );

            throw new SupportTicketException("Unable to load trader profile with ID: " + traderId);
        }
    }

    private String extractBearerToken() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            throw new SupportTicketException("Current HTTP request context not found.");
        }

        String authorizationHeader = attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new SupportTicketException("Authorization token not found.");
        }

        return authorizationHeader.substring("Bearer ".length());
    }
}
