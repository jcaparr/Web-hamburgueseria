package com.hamburguesas.dto;

import java.util.List;

/**
 * El buzón de notificaciones (#210): los avisos más recientes, mezclados y por fecha.
 *
 * @param nuevas cuántos de esos llegaron desde la última vez que lo abrió
 */
public record BuzonDto(List<NotificacionDto> notificaciones, long nuevas) {}
