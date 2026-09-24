package it.giuval.cloud.telemetry_services.util;

import it.giuval.cloud.telemetry_services.domain.Position;

public final class GeoUtils {

	private static final double EARTH_RADIUS_KM = 6371.0;

	//Formula di distanza — Haversine (standard per coordinate geografiche)
	public static double distanceKm(Position position1, Position position2) {
		double dLat = Math.toRadians(position2.latitude() - position1.latitude());
		double dLon = Math.toRadians(position2.longitude() - position1.longitude());

		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(position1.latitude())) * Math.cos(Math.toRadians(position2.latitude()))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);

		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

		return EARTH_RADIUS_KM * c;
	}
}